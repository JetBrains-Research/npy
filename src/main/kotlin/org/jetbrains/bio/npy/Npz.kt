package org.jetbrains.bio.npy

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
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
            val header = NpyFile.Header.read(zf.getBuffer(it))
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

            NpzEntry(it.name.substringBeforeLast('.'), type, header.shape.single())
        }.toList()

        /**
         * Returns an array for a given name.
         *
         * The caller is responsible for casting the resulting array to an
         * appropriate type.
         */
        operator fun get(name: String): Any {
            return NpyFile.read(zf.getBuffer(zf.getEntry(name + ".npy")))
        }

        private fun ZipFile.getBuffer(entry: ZipEntry): ByteBuffer {
            val input = ByteBuffer.allocate(entry.size.toInt())
            getInputStream(entry).use {
                Channels.newChannel(it).read(input)
            }

            input.rewind()
            return input.asReadOnlyBuffer()
        }

        override fun close() = zf.close()
    }

    /** Opens an NPZ file at [path] for reading. */
    @JvmStatic fun read(path: Path) = Reader(path)

    /**
     * A writer for NPZ format.
     *
     * The implementation uses a temporary [ByteBuffer] to store the
     * serialized array prior to archiving. Thus each [.write] call
     * requires N extra bytes of memory for an array of N bytes.
     */
    data class Writer internal constructor(val path: Path, val compressed: Boolean) :
            Closeable, AutoCloseable {

        private val zos = ZipOutputStream(Files.newOutputStream(path).buffered(),
                                          Charsets.US_ASCII)

        fun write(name: String, data: BooleanArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: ByteArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: ShortArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: IntArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: LongArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: FloatArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: DoubleArray) = withEntry(name) { NpyFile.allocate(data) }

        fun write(name: String, data: Array<String>) = withEntry(name) { NpyFile.allocate(data) }

        private inline fun withEntry(name: String, block: () -> ByteBuffer) {
            val output = block()
            output.rewind()

            val entry = ZipEntry(name + ".npy").apply {
                if (compressed) {
                    method = ZipEntry.DEFLATED
                } else {
                    method = ZipEntry.STORED
                    size = output.capacity().toLong()
                    crc = CRC32().apply { update(output) }.value
                    output.rewind()
                }
            }

            zos.putNextEntry(entry)
            try {
                val fc = Channels.newChannel(zos)
                while (output.hasRemaining()) {
                    fc.write(output)
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
data class NpzEntry(val name: String, val type: Class<*>, val size: Int)
