package org.jetbrains.bio.npy

import com.google.common.primitives.Shorts
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.lang.ProcessBuilder.Redirect
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpyFileTest {
    @Test fun testWriteReadBooleans() = withTempFile("test", ".npy") { path ->
        val data = booleanArrayOf(true, true, true, false)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as BooleanArray)
    }

    @Test fun testWriteReadBytes() = withTempFile("test", ".npy") { path ->
        val data = byteArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as ByteArray)
    }

    @Test fun testWriteReadShorts() = withTempFile("test", ".npy") { path ->
        val data = shortArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as ShortArray)
    }

    @Test fun testWriteReadInts() = withTempFile("test", ".npy") { path ->
        val data = intArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as IntArray)
    }

    @Test fun testWriteReadLongs() = withTempFile("test", ".npy") { path ->
        val data = longArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as LongArray)
    }

    @Test fun testWriteReadFloats() = withTempFile("test", ".npy") { path ->
        val data = floatArrayOf(1f, 2f, 3f, 4f)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as FloatArray, Math.ulp(1f))
    }

    @Test fun testWriteReadDoubles() = withTempFile("test", ".npy") { path ->
        val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as DoubleArray, Math.ulp(1.0))
    }

    @Suppress("unchecked_cast")
    @Test fun testWriteReadStrings() = withTempFile("test", ".npy") { path ->
        val data = arrayOf("foo", "bar", "bazooka")
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path) as Array<String>)
    }
}

class NpyFileHeaderTest {
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

            FileChannel.open(path, StandardOpenOption.WRITE).use {
                val output = header.allocate()
                output.rewind()
                it.write(output)
            }

            FileChannel.open(path).use {
                val input = it.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path))
                assertEquals(header, NpyFile.Header.read(input))
            }
        }
    }
}

class NpyFileNumPyTest {
    private val hasNumPy: Boolean get() {
        val (rc, _output) = command("python", "-c", "import numpy")
        return rc == 0
    }

    @Test fun testWriteRead() {
        if (!hasNumPy) {
            return
        }

        withTempFile("test", ".npy") { path ->
            val data = intArrayOf(1, 2, 3, 4)
            NpyFile.write(path, data)
            val (rc, output) = command(
                    "python", "-c", "import numpy as np; print(np.load('$path'))")
            assertEquals(0, rc)
            assertEquals("[1 2 3 4]", output.trim())
        }
    }

    private fun command(vararg args: String): Pair<Int, String> {
        val p = ProcessBuilder()
                .command(*args)
                .redirectOutput(Redirect.PIPE)
                .start()

        val rc = p.waitFor()
        return rc to p.inputStream.bufferedReader().readText()
    }
}
