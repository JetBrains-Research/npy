package org.jetbrains.bio.npy

import com.google.common.primitives.Shorts
import org.junit.Test
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpyFileTest {
    @Test fun metaIsPadded() {
        val header = NpyFile.Header(major = 1, minor = 0,
                                    type = 'i', bytes = 4,
                                    shape = intArrayOf(42))
        val headerSize = NpyFile.Header.MAGIC.size + 2 + Shorts.BYTES + header.meta.size
        assertTrue(headerSize % 16 == 0)
    }

    @Test fun writeRead10() = testWriteRead(1, 0)

    @Test fun writeRead20() = testWriteRead(2, 0)

    fun testWriteRead(major: Int, minor: Int) {
        withTempFile("test", ".npz") { path ->
            val header = NpyFile.Header(major = major, minor = minor,
                                        type = 'i', bytes = 4,
                                        shape = intArrayOf(42))

            RandomAccessFile(path.toFile(), "rw").use {
                it.write(header.allocate().array())
            }

            FileChannel.open(path).use {
                val input = it.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path))
                assertEquals(header, NpyFile.Header.read(input))
            }
        }
    }
}