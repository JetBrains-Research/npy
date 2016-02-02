package org.jetbrains.bio.npy

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.Deflater
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
    private val zf = ZipFile(path.toFile())

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
        return zf.getInputStream(zf.getEntry(name + ".npy")).use {
            NpyFile.read(DataInputStream(it.buffered()))
        }
    }

    override fun close() = zf.close()

    class Builder(val path: Path,
                  compression: Int = Deflater.NO_COMPRESSION)
    :
            Closeable, AutoCloseable {

        private val zos = ZipOutputStream(Files.newOutputStream(path).buffered(),
                                          Charsets.US_ASCII)

        init {
            zos.setLevel(compression)
        }

        fun add(name: String, data: BooleanArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: ByteArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: ShortArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: IntArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: LongArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: FloatArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: DoubleArray) = withEntry(name) { NpyFile.write(it, data) }

        fun add(name: String, data: Array<String>) = withEntry(name) { NpyFile.write(it, data) }

        private inline fun withEntry(name: String, block: (DataOutput) -> Unit) {
            val entry = ZipEntry(name + ".npy")
            try {
                zos.putNextEntry(entry)
                block(DataOutputStream(zos))
            } finally {
                zos.closeEntry()
            }
        }

        override fun close() = zos.close()
    }

    companion object {
        /** Creates an NPZ file at [path] and populates it from a closure. */
        inline fun create(path: Path, block: Builder.() -> Unit) {
            Builder(path).use { it.apply(block) }
        }
    }
}
