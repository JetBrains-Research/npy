package org.jetbrains.bio.npy

import java.io.Closeable
import java.io.File
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
data class NpzFile(val path: Path) : Closeable, AutoCloseable {
    private val zf = ZipFile(path.toFile(), ZipFile.OPEN_READ, Charsets.US_ASCII)

    /** Lists arrays available in a file. */
    fun list() = zf.entries().asSequence()
            .map { File(it.name).nameWithoutExtension }.toList()

    /**
     * Returns an array for a given name.
     *
     * The caller is responsible for casting the resulting array to an
     * appropriate type.
     */
    operator fun get(name: String): Any {
        val entry = zf.getEntry(name + ".npy")
        val input = ByteBuffer.allocate(entry.size.toInt())
        zf.getInputStream(entry).use {
            Channels.newChannel(it).read(input)
        }

        input.rewind()
        return NpyFile.read(input.asReadOnlyBuffer())
    }

    override fun close() = zf.close()

    /**
     * A writer for NPZ format.
     *
     * The implementation uses a temporary [ByteBuffer] to store the
     * serialized array prior to archiving. Thus each [.write] call
     * requires N extra bytes of memory for an array of N bytes.
     */
    class Writer internal constructor(val path: Path, val compressed: Boolean) :
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

    companion object {
        /** Creates an NPZ file at [path] and populates it from a closure. */
        @JvmStatic fun create(path: Path, compressed: Boolean = false,
                              block: Writer.() -> Unit) {
            Writer(path, compressed).use { it.apply(block) }
        }
    }
}
