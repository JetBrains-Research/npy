package org.jetbrains.bio.npy

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.IOError
import java.lang.ProcessBuilder.Redirect
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class NpyFileTest(private val order: ByteOrder) {
    @Test fun writeReadBooleans() = withTempFile("test", ".npy") { path ->
        val data = booleanArrayOf(true, true, true, false)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path).asBooleanArray())
    }

    @Test fun writeReadBytes() = withTempFile("test", ".npy") { path ->
        val data = byteArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path).asByteArray())
    }

    @Test fun writeReadShorts() = withTempFile("test", ".npy") { path ->
        val data = shortArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data, order = order)
        assertArrayEquals(data, NpyFile.read(path).asShortArray())
    }

    @Test fun writeReadInts() = withTempFile("test", ".npy") { path ->
        val data = intArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data, order = order)
        assertArrayEquals(data, NpyFile.read(path).asIntArray())
    }

    @Test fun writeReadLongs() = withTempFile("test", ".npy") { path ->
        val data = longArrayOf(1, 2, 3, 4)
        NpyFile.write(path, data, order = order)
        assertArrayEquals(data, NpyFile.read(path).asLongArray())
    }

    @Test fun writeReadFloats() = withTempFile("test", ".npy") { path ->
        val data = floatArrayOf(1f, 2f, 3f, 4f)
        NpyFile.write(path, data, order = order)
        assertArrayEquals(data, NpyFile.read(path).asFloatArray(), Math.ulp(1f))
    }

    @Test fun writeReadDoubles() = withTempFile("test", ".npy") { path ->
        val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        NpyFile.write(path, data, order = order)
        assertArrayEquals(data, NpyFile.read(path).asDoubleArray(), Math.ulp(1.0))
    }

    @Suppress("unchecked_cast")
    @Test fun writeReadStrings() = withTempFile("test", ".npy") { path ->
        val data = arrayOf("foo", "bar", "bazooka")
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path).asStringArray())
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun `data`(): Collection<Any> = listOf(ByteOrder.BIG_ENDIAN,
                                               ByteOrder.LITTLE_ENDIAN)
    }
}

@RunWith(Parameterized::class)
class NpyFileNDTest(private val shape: IntArray) {
    @Test fun writeReadDouble() = withTempFile("test", ".npy") { path ->
        val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0)
        NpyFile.write(path, data, shape = shape)
        val serialized = NpyFile.read(path)
        assertArrayEquals(shape, serialized.shape)
        assertArrayEquals(data, serialized.asDoubleArray(), Math.ulp(1.0))
    }

    companion object {
        @JvmStatic @Parameters fun `data`() = listOf(
                intArrayOf(1, 8),
                intArrayOf(8, 1),
                intArrayOf(2, 4),
                intArrayOf(4, 2),
                intArrayOf(2, 2, 2),
                intArrayOf(2, 2, 2, 1))
    }
}

class NpyFileHeaderTest {
    @Test fun isPadded() {
        val header = NpyFile.Header(type = 'i', bytes = 4, shape = intArrayOf(42))
        assertEquals(1, header.major)
        assertTrue(header.allocate().capacity() % 16 == 0)
    }

    @Test fun writeRead10() = testWriteRead(65536)

    @Test fun writeRead20() = testWriteRead(16)  // force 2.0

    fun testWriteRead(boundary: Int = NpyFile.Header.NPY_10_20_SIZE_BOUNDARY) {
        withTempFile("test", ".npz") { path ->
            val backup = NpyFile.Header.NPY_10_20_SIZE_BOUNDARY
            NpyFile.Header.NPY_10_20_SIZE_BOUNDARY = boundary
            val header = NpyFile.Header(type = 'i', bytes = 4, shape = intArrayOf(0))
            NpyFile.Header.NPY_10_20_SIZE_BOUNDARY = backup

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
        try {
            val (rc, _) = command("python", "-c", "import numpy")
            return rc == 0
        } catch(e: IOError) {
            return false  // No Python?
        }
    }

    @Test fun writeRead() {
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

class NpyFileStressTest {
    @Test(expected = IllegalStateException::class)
    fun readRandomGibberish() = withTempFile("test", ".npy") { path ->
        val r = Random()
        Files.write(path, ByteArray(65536) { r.nextInt().toByte() })
        NpyFile.read(path)
    }

    @Test fun writeReadRandom() {
        val r = Random()
        val maxMemory = Runtime.getRuntime().maxMemory() / java.lang.Double.BYTES
        val maxSize = Math.toIntExact(maxMemory / 100)  // 1%
        for (i in 0 until 10) {
            val size = r.nextInt(maxSize).toLong()
            val data = r.doubles(size).toArray()

            withTempFile("test", ".npy") { path ->
                NpyFile.write(path, data)

                assertArrayEquals(data, NpyFile.read(path).asDoubleArray(),
                                  Math.ulp(1.0))
            }
        }
    }

    @Test fun writeReadUnaligned() = withTempFile("test", ".npy") { path ->
        val data = Random().doubles(65536).toArray()
        NpyFile.write(path, data)
        assertArrayEquals(data, NpyFile.read(path, step=123).asDoubleArray(),
                          Math.ulp(1.0))
    }
}

class NpyFileNonRegressionTest {
    @Test fun readLongShapePython2() {
        // See https://github.com/JetBrains-Research/npy/pull/6.
        assertArrayEquals(
                doubleArrayOf(1.0, 1.0),
                NpyFile.read(Examples["win64python2.npy"]).asDoubleArray(),
                Math.ulp(1.0))
    }
}
