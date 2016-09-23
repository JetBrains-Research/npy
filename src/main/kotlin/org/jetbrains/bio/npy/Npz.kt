package org.jetbrains.bio.npy

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * A ZIP file where each individual file is in NPY format.
 *
 * By convention each file has `.npy` extension, however, the API
 * doesn't expose it. So for instance the array named "X" will be
 * accessibly via "X" and **not** "X.npy".
 *
 * Example:
 *
 * @sample [org.jetbrains.bio.npy.npzExample]
 */
object NpzFile {
    /**
     * A reader for NPZ format.
     *
     * @since 0.2.0
     */
    data class Reader internal constructor(val path: Path): Closeable, AutoCloseable {
        private val zf = ZipFile(path.toFile(), ZipFile.OPEN_READ, Charsets.US_ASCII)

        /**
         * Returns a mapping from array names to the corresponding
         * scalar types in Java.
         *
         * @since 0.2.0
         */
        fun introspect() = zf.entries().asSequence().map {
            val header = NpyFile.Header.read(zf.getBuffers(it, Integer.MAX_VALUE).first())
            val type = when (header.type) {
                'b' -> Boolean::class.java
                'i', 'u' -> when (header.bytes) {
                    1 -> Byte::class.java
                    2 -> Short::class.java
                    4 -> Int::class.java
                    8 -> Long::class.java
                    else -> impossible()
                }
                'f' -> when (header.bytes) {
                    4 -> Float::class.java
                    8 -> Double::class.java
                    else -> impossible()
                }
                'S' -> String::class.java
                else -> impossible()
            }

            NpzEntry(it.name.substringBeforeLast('.'), type, header.shape)
        }.toList()

        /**
         * Returns an array for a given name.
         *
         * The caller is responsible for casting the resulting array to an
         * appropriate type.
         *
         * @param name array name.
         * @param step amount of bytes to use for the temporary buffer, when
         *             reading the entry. Defaults to 1 << 18 to mimic NumPy
         *             behaviour.
         */
        operator fun get(name: String, step: Int = 1 shl 18): NpyArray {
            return NpyFile.read(zf.getBuffers(zf.getEntry(name + ".npy"), step))
        }

        private fun ZipFile.getBuffers(entry: ZipEntry, step: Int): Sequence<ByteBuffer> {
            val `is` = getInputStream(entry)
            var chunk = ByteBuffer.allocate(0)
            return generateSequence {
                val remaining = `is`.available() + chunk.remaining()
                if (remaining > 0) {
                    chunk = ByteBuffer.allocate(Math.min(remaining, step)).apply {
                        // Make sure we don't miss any unaligned bytes.
                        put(chunk)
                        Channels.newChannel(`is`).read(this)
                        rewind()
                    }.asReadOnlyBuffer()

                    chunk
                } else {
                    `is`.close()
                    null
                }
            }
        }

        override fun close() = zf.close()
    }

    /** Opens an NPZ file at [path] for reading. */
    @JvmStatic fun read(path: Path) = Reader(path)

    /**
     * A writer for NPZ format.
     *
     * The implementation uses a temporary [ByteBuffer] to store the
     * serialized array prior to archiving. Thus each [write] call
     * requires N extra bytes of memory for an array of N bytes.
     */
    data class Writer internal constructor(val path: Path, val compressed: Boolean) :
            Closeable, AutoCloseable {

        private val zos = ZipOutputStream(Files.newOutputStream(path).buffered(),
                                          Charsets.US_ASCII)

        @JvmOverloads
        fun write(name: String, data: BooleanArray, shape: IntArray = intArrayOf(data.size)) {
            writeEntry(name, NpyFile.allocate(data, shape))
        }

        @JvmOverloads
        fun write(name: String, data: ByteArray, shape: IntArray = intArrayOf(data.size)) {
            writeEntry(name, NpyFile.allocate(data, shape))
        }

        @JvmOverloads
        fun write(name: String, data: ShortArray, shape: IntArray = intArrayOf(data.size),
                  order: ByteOrder = ByteOrder.nativeOrder()) {
            writeEntry(name, NpyFile.allocate(data, shape, order))
        }

        @JvmOverloads
        fun write(name: String, data: IntArray, shape: IntArray = intArrayOf(data.size),
                  order: ByteOrder = ByteOrder.nativeOrder()) {
            writeEntry(name, NpyFile.allocate(data, shape, order))
        }

        @JvmOverloads
        fun write(name: String, data: LongArray, shape: IntArray = intArrayOf(data.size),
                  order: ByteOrder = ByteOrder.nativeOrder()) {
            writeEntry(name, NpyFile.allocate(data, shape, order))
        }

        @JvmOverloads
        fun write(name: String, data: FloatArray, shape: IntArray = intArrayOf(data.size),
                  order: ByteOrder = ByteOrder.nativeOrder()) {
            writeEntry(name, NpyFile.allocate(data, shape, order))
        }

        @JvmOverloads
        fun write(name: String, data: DoubleArray, shape: IntArray = intArrayOf(data.size),
                  order: ByteOrder = ByteOrder.nativeOrder()) {
            writeEntry(name, NpyFile.allocate(data, shape, order))
        }

        @JvmOverloads
        fun write(name: String, data: Array<String>, shape: IntArray = intArrayOf(data.size)) {
            writeEntry(name, NpyFile.allocate(data, shape))
        }

        private fun writeEntry(name: String, chunks: Sequence<ByteBuffer>) {
            val entry = ZipEntry(name + ".npy").apply {
                if (compressed) {
                    method = ZipEntry.DEFLATED
                } else {
                    method = ZipEntry.STORED

                    val crcAcc = CRC32()
                    var sizeAcc = 0L
                    for (chunk in chunks) {
                        sizeAcc += chunk.capacity().toLong()
                        crcAcc.update(chunk)
                        chunk.rewind()
                    }

                    size = sizeAcc
                    crc = crcAcc.value
                }
            }

            zos.putNextEntry(entry)
            try {
                val fc = Channels.newChannel(zos)
                for (chunk in chunks) {
                    while (chunk.hasRemaining()) {
                        fc.write(chunk)
                    }
                }
            } finally {
                zos.closeEntry()
            }
        }

        override fun close() = zos.close()
    }

    /** Opens an NPZ file at [path] for writing. */
    @JvmStatic fun write(path: Path, compressed: Boolean = false): Writer {
        return Writer(path, compressed)
    }
}

/** A stripped down NPY header for an array in NPZ. */
class NpzEntry(val name: String, val type: Class<*>, val shape: IntArray) {
    override fun toString() = StringJoiner(", ", "NpzEntry{", "}")
            .add("name=" + name)
            .add("type=" + type)
            .add("shape=" + Arrays.toString(shape))
            .toString()
}

/** This function is for documentation purposes only. */
internal fun npzExample() {
    val values1 = intArrayOf(1, 2, 3, 4, 5, 6)
    val values2 = booleanArrayOf(true, false)
    val path = Paths.get("sample.npz")

    NpzFile.write(path).use {
        it.write("xs", values1, shape = intArrayOf(2, 3))
        it.write("mask", values2)
    }

    NpzFile.read(path).use {
        println(it.introspect())
        // => [NpzEntry{name=xs, type=int, shape=[2, 3]},
        //     NpzEntry{name=mask, type=boolean, shape=[2]}]

        println("xs   = ${it["xs"]}")
        println("mask = ${it["mask"]}")
        // => xs   = NpyArray{data=[1, 2, 3, 4, 5, 6], shape=[2, 3]}
        //    mask = NpyArray{data=[true, false], shape=[2]}
    }
}