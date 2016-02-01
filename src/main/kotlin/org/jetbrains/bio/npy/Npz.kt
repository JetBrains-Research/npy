package org.jetbrains.bio.npy

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * A ZIP file where each individual file is in NPY format.
 */
data class NpzFile(val path: Path) : Closeable, AutoCloseable {
    private val zf = ZipFile(path.toFile())

    fun list() = zf.entries().asSequence()
            .map { File(it.name).nameWithoutExtension }.toList()

    operator fun get(name: String): Any {
        return zf.getInputStream(zf.getEntry(name + ".npy")).use {
            NpyFile.read(DataInputStream(it))
        }
    }

    override fun close() = zf.close()

    class Builder(val path: Path) {
        private val zos = ZipOutputStream(Files.newOutputStream(path))

        fun add(name: String, data: BooleanArray) = withEntry(name) {
            NpyFile.write(DataOutputStream(zos), data)
        }

        private inline fun withEntry(name: String, block: () -> Unit) {
            val entry = ZipEntry(name + ".npy")
            zos.putNextEntry(entry)
            block()
            zos.closeEntry()
        }

        fun close() {
            zos.close()
        }
    }
}
