package org.jetbrains.bio.npy

import java.io.Closeable
import java.io.DataInputStream
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

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
}