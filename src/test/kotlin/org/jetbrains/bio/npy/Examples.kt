package org.jetbrains.bio.npy

import java.nio.file.Path
import java.nio.file.Paths

internal object Examples {
    @JvmStatic operator fun get(name: String): Path {
        val url = Examples.javaClass.classLoader.getResource(name)
                  ?: throw IllegalStateException("resource not found")

        return Paths.get(url.toURI()).toFile().toPath()
    }
}