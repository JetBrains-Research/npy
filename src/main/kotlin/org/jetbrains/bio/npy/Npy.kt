package org.jetbrains.bio.npy

import com.google.common.base.Charsets
import com.google.common.base.MoreObjects
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.primitives.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * A file in NPY format.
 *
 * Currently unsupported types:
 *
 *   * unsigned integral types (treated as signed)
 *   * bit field,
 *   * complex,
 *   * object,
 *   * Unicode
 *   * void*
 *   * intersections aka types for structured arrays.
 *
 * See http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html
 *
 * Example:
 *
 * @sample [org.jetbrains.bio.npy.npyExample]
 */
object NpyFile {
    /**
     * NPY file header.
     *
     * Presently NumPy implements two version of the NPY format: 1.0 and 2.0.
     * The difference between the two is the maximum size of the NPY header.
     * Version 1.0 requires it to be <=2**16 while version 2.0 allows <=2**32.
     *
     * By default a more common 1.0 format is used.
     */
    internal data class Header(val major: Int = 1, val minor: Int = 0,
                               val order: ByteOrder? = null,
                               val type: Char, val bytes: Int,
                               val shape: IntArray) {
        init {
            require((major == 1 || major == 2) && minor == 0) {
                "version must be 1.0 or 2.0"
            }
        }

        val meta: ByteArray by lazy {
            val descr = "${order.toChar()}$type$bytes"
            val metaUnpadded =
                    "{'descr': '$descr', 'fortran_order': False, 'shape': (${shape.joinToString(",")}, ), }\n"

            // According to the spec the total header size should be
            // evenly divisible by 16 for alignment purposes.
            val totalUnpadded = MAGIC.size + 2 + when (major to minor) {
                1 to 0 -> 2
                2 to 0 -> 4
                else -> impossible()
            } + metaUnpadded.length

            Strings.padEnd(metaUnpadded,
                           metaUnpadded.length + (16 - totalUnpadded % 16),
                           ' ')
                    .toByteArray(Charsets.US_ASCII)
        }

        /**
         * Allocates a [ByteBuffer] for the contents described by the header.
         *
         * The returned buffer already contains the serialized header and is
         * guaranteed to be in correct [ByteOrder].
         */
        fun allocate(): ByteBuffer {
            val total = MAGIC.size + 2 + when (major to minor) {
                1 to 0 -> 2
                2 to 0 -> 4
                else   -> impossible()
            } + meta.size

            return ByteBuffer.allocateDirect(total).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put(MAGIC)
                put(major.toByte())
                put(minor.toByte())

                when (major to minor) {
                    1 to 0 -> putShort(Shorts.checkedCast(meta.size.toLong()))
                    2 to 0 -> putInt(meta.size)
                }

                put(meta)
                rewind()
            }
        }

        override fun equals(other: Any?) = when {
            this === other -> true
            other == null || other !is Header -> false
            else -> {
                major == other.major && minor == other.minor &&
                order == other.order &&
                type == other.type && bytes == other.bytes &&
                Arrays.equals(shape, other.shape)
            }
        }

        override fun hashCode() = Objects.hash(major, minor, order, type, bytes,
                                               Arrays.hashCode(shape))

        companion object {
            /** Each NPY file *must* start with this byte sequence. */
            val MAGIC = byteArrayOf(0x93.toByte()) + "NUMPY".toByteArray()

            @Suppress("unchecked_cast")
            fun read(input: ByteBuffer) = with(input.order(ByteOrder.LITTLE_ENDIAN)) {
                val buf = ByteArray(6)
                get(buf)
                check(Arrays.equals(MAGIC, buf)) { "bad magic: ${String(buf)}" }

                val major = get().toInt()
                val minor = get().toInt()
                val size = when (major to minor) {
                    1 to 0 -> getShort().toInt()
                    2 to 0 -> getInt()
                    else -> error("unsupported version: $major.$minor")
                }

                val header = ByteArray(size)
                get(header)

                val meta = parseDict(String(header))
                val dtype = meta["descr"] as String
                check(!(meta["fortran_order"] as Boolean)) {
                    "Fortran-contiguous arrays are not supported"
                }

                val shape = (meta["shape"] as List<Int>).toIntArray()
                val order = dtype[0].toByteOrder()
                Header(major, minor, order = order,
                       type = dtype[1], bytes = dtype.substring(2).toInt(),
                       shape = shape)
            }
        }
    }

    /**
     * Reads an array in NPY format from a given path.
     *
     * The caller is responsible for coercing the resulting array to
     * an appropriate type via [NpyArray] methods.
     */
    @JvmStatic fun read(path: Path): NpyArray = FileChannel.open(path).use {
        read(it.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path)))
    }

    internal fun read(input: ByteBuffer): NpyArray {
        val header = Header.read(input)
        val size = header.shape.product()
        check(input.remaining() == header.bytes * size)
        val data: Any = with(input.order(header.order)) {
            when (header.type) {
                'b' -> {
                    check(header.bytes == 1)
                    BooleanArray(size) { get() == 1.toByte() }
                }
                'u', 'i' -> when (header.bytes) {
                    1 -> ByteArray(size).apply { get(this) }
                    2 -> ShortArray(size).apply { asShortBuffer().get(this) }
                    4 -> IntArray(size).apply { asIntBuffer().get(this) }
                    8 -> LongArray(size).apply { asLongBuffer().get(this) }
                    else -> error("invalid number of bytes for ${header.type}: ${header.bytes}")
                }
                'f' -> when (header.bytes) {
                    4 -> FloatArray(size).apply { asFloatBuffer().get(this) }
                    8 -> DoubleArray(size).apply { asDoubleBuffer().get(this) }
                    else -> error("invalid number of bytes for ${header.type}: ${header.bytes}")
                }
                'S' -> Array(size) {
                    val s = ByteArray(header.bytes)
                    get(s)
                    String(s, Charsets.US_ASCII).trim { it == '\u0000' }
                }
                else -> error("unsupported type: ${header.type}")
            }
        }

        return NpyArray(data, header.shape)
    }

    /**
     * Writes an array in NPY format to a given path.
     */
    @JvmOverloads
    @JvmStatic fun write(path: Path, data: BooleanArray,
                         shape: IntArray = intArrayOf(data.size)) {
        write(path, allocate(data, shape))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: ByteArray,
                         shape: IntArray = intArrayOf(data.size)) {
        write(path, allocate(data, shape))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: ShortArray,
                         shape: IntArray = intArrayOf(data.size),
                         order: ByteOrder = ByteOrder.nativeOrder()) {
        write(path, allocate(data, shape, order))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: IntArray,
                         shape: IntArray = intArrayOf(data.size),
                         order: ByteOrder = ByteOrder.nativeOrder()) {
        write(path, allocate(data, shape, order))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: LongArray,
                         shape: IntArray = intArrayOf(data.size),
                         order: ByteOrder = ByteOrder.nativeOrder()) {
        write(path, allocate(data, shape, order))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: FloatArray,
                         shape: IntArray = intArrayOf(data.size),
                         order: ByteOrder = ByteOrder.nativeOrder()) {
        write(path, allocate(data, shape, order))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: DoubleArray,
                         shape: IntArray = intArrayOf(data.size),
                         order: ByteOrder = ByteOrder.nativeOrder()) {
        write(path, allocate(data, shape, order))
    }

    @JvmOverloads
    @JvmStatic fun write(path: Path, data: Array<String>,
                         shape: IntArray = intArrayOf(data.size)) {
        write(path, allocate(data, shape))
    }

    private fun write(path: Path, chunks: Iterable<ByteBuffer>) {
        FileChannel.open(path,
                         StandardOpenOption.WRITE,
                         StandardOpenOption.CREATE).use {

            for (chunk in chunks) {
                while (chunk.hasRemaining()) {
                    it.write(chunk)
                }
            }
        }
    }

    internal fun allocate(data: BooleanArray, shape: IntArray): Iterable<ByteBuffer> {
        val header = Header(order = null, type = 'b', bytes = 1, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                BooleanArraySplitBuffer(data))
    }

    internal fun allocate(data: ByteArray, shape: IntArray): Iterable<ByteBuffer> {
        val header = Header(order = null, type = 'i', bytes = 1, shape = shape)
        return listOf(header.allocate(), ByteBuffer.wrap(data))
    }

    internal fun allocate(data: ShortArray, shape: IntArray,
                          order: ByteOrder): Iterable<ByteBuffer> {
        val header = Header(order = order, type = 'i', bytes = Shorts.BYTES, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                ShortArraySplitBuffer(data, order))
    }

    internal fun allocate(data: IntArray, shape: IntArray,
                          order: ByteOrder): Iterable<ByteBuffer> {
        val header = Header(order = order, type = 'i', bytes = Ints.BYTES, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                IntArraySplitBuffer(data, order))
    }

    internal fun allocate(data: LongArray, shape: IntArray,
                          order: ByteOrder): Iterable<ByteBuffer> {
        val header = Header(order = order, type = 'i', bytes = Longs.BYTES, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                LongArraySplitBuffer(data, order))
    }

    internal fun allocate(data: FloatArray, shape: IntArray,
                          order: ByteOrder): Iterable<ByteBuffer> {
        val header = Header(order = order, type = 'f', bytes = Floats.BYTES, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                FloatArraySplitBuffer(data, order))
    }

    internal fun allocate(data: DoubleArray, shape: IntArray,
                          order: ByteOrder): Iterable<ByteBuffer> {
        val header = Header(order = order, type = 'f', bytes = Doubles.BYTES, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                DoubleArraySplitBuffer(data, order))
    }

    internal fun allocate(data: Array<String>, shape: IntArray): Iterable<ByteBuffer> {
        val bytes = data.asSequence().map { it.length }.max() ?: 0
        val header = Header(order = null, type = 'S', bytes = bytes, shape = shape)
        return Iterables.concat(listOf(header.allocate()),
                                StringArraySplitBuffer(data))
    }
}

/** A wrapper for NPY array data. */
class NpyArray(
        /** Array data. */
        val data: Any,
        /** Array dimensions. */
        val shape: IntArray) {

    fun asBooleanArray() = data as BooleanArray

    fun asByteArray() = data as ByteArray

    fun asShortArray() = data as ShortArray

    fun asIntArray() = data as IntArray

    fun asLongArray() = data as LongArray

    fun asFloatArray() = data as FloatArray

    fun asDoubleArray() = data as DoubleArray

    @Suppress("unchecked_cast")
    fun asStringArray() = data as Array<String>

    override fun toString() = MoreObjects.toStringHelper(this)
            .add("data", Arrays.deepToString(arrayOf(data))
                    .removeSurrounding("[", "]"))
            .add("shape", Arrays.toString(shape))
            .toString()
}

private fun Char.toByteOrder() = when (this) {
    '<'  -> ByteOrder.LITTLE_ENDIAN
    '>'  -> ByteOrder.BIG_ENDIAN
    '|'  -> null
    else -> error(this)
}

private fun ByteOrder?.toChar() = when (this) {
    ByteOrder.LITTLE_ENDIAN -> '<'
    ByteOrder.BIG_ENDIAN -> '>'
    null -> '|'
    else -> error(this)
}

/** This function is for documentation purposes only. */
internal fun npyExample() {
    val values = intArrayOf(1, 2, 3, 4, 5, 6)
    val path = Paths.get("sample.npy")
    NpyFile.write(path, values, shape = intArrayOf(2, 3))

    println(NpyFile.read(path))
    // => NpyArray{data=[1, 2, 3, 4, 5, 6], shape=[2, 3]}
}
