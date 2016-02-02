package org.jetbrains.bio.npy

import com.google.common.base.CharMatcher
import com.google.common.base.Charsets
import com.google.common.base.Splitter
import com.google.common.base.Strings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * A file in NPY format.
 *
 * Currently unsupported types:
 * * bit field,
 * * complex,
 * * object,
 * * Unicode
 * * void*
 * * intersections aka types for structured arrays.
 *
 * See http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html
 */
class NpyFile {
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
                               val order: ByteOrder? = ByteOrder.nativeOrder(),
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
                    "{'descr': '$descr', 'fortran_order': False, 'shape': (${shape.joinToString(",")}), }\n"

            // According to the spec the total header size should be
            // evenly divisible by 16 for alignment purposes.
            val totalUnpadded = MAGIC.size + 2 + when (major to minor) {
                1 to 0 -> 2
                2 to 0 -> 4
                else -> TODO()
            } + metaUnpadded.length

            Strings.padEnd(metaUnpadded, metaUnpadded.length + totalUnpadded % 16, ' ')
                    .toByteArray(Charsets.US_ASCII)
        }

        /**
         * Allocates a [ByteBuffer] for the contents described by the header.
         *
         * The returned buffer already contains the serialized header and is
         * guaranteed to be in correct [ByteOrder].
         */
        fun allocate(): ByteBuffer {
            val (size) = shape
            val total = MAGIC.size + 2 + when (major to minor) {
                    1 to 0 -> 2
                    2 to 0 -> 4
                    else -> kotlin.TODO()
            } + meta.size + bytes * size

            return ByteBuffer.allocate(total).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put(MAGIC)
                put(major.toByte())
                put(minor.toByte())

                when (major to minor) {
                    1 to 0 -> {
                        check(meta.size <= Short.MAX_VALUE)
                        putShort(meta.size.toShort())
                    }
                    2 to 0 -> putInt(meta.size)
                }

                put(meta)
                order(order ?: ByteOrder.nativeOrder())
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
            internal val MAGIC = byteArrayOf(0x93.toByte()) + "NUMPY".toByteArray()

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

                val normalized = CharMatcher.WHITESPACE.or(CharMatcher.anyOf("'{}()"))
                        .removeFrom(String(header, Charsets.US_ASCII).trimEnd(','))
                val meta = Splitter.on(',').omitEmptyStrings()
                        .withKeyValueSeparator(':')
                        .split(normalized)

                val dtype = meta["descr"]!!
                check(!meta["fortran_order"]!!.toBoolean()) {
                    "Fortran-contiguous arrays are not supported"
                }

                val shape = meta["shape"]!!.split(',').map { it.toInt() }.toIntArray()
                check(shape.size == 1) {
                    "Multidimensional arrays are not supported"
                }

                val order = dtype[0].toByteOrder()
                Header(major, minor, order = order,
                       type = dtype[1], bytes = dtype.substring(2).toInt(),
                       shape = shape)
            }
        }
    }

    companion object {
        fun read(input: ByteBuffer): Any {
            val header = Header.read(input)
            val (size) = header.shape
            check(input.remaining() == header.bytes * size)
            return with(input.order(header.order)) {
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
                        String(s, Charsets.US_ASCII)
                    }
                    else -> error("unsupported type: ${header.type}")
                }
            }
        }

        fun allocate(data: BooleanArray): ByteBuffer {
            val header = Header(order = null, type = 'b', bytes = 1, shape = intArrayOf(data.size))
            return header.allocate().apply {
                data.forEach { put(if (it) 1.toByte() else 0.toByte()) }
            }
        }

        fun allocate(data: ByteArray): ByteBuffer {
            return Header(type = 'i', bytes = 1, shape = intArrayOf(data.size))
                    .allocate().put(data)
        }

        fun allocate(data: ShortArray): ByteBuffer {
            return Header(type = 'i', bytes = 2, shape = intArrayOf(data.size))
                    .allocate().apply { asShortBuffer().put(data) }
        }

        fun allocate(data: IntArray): ByteBuffer {
            return Header(type = 'i', bytes = 4, shape = intArrayOf(data.size))
                    .allocate().apply { asIntBuffer().put(data) }
        }

        fun allocate(data: LongArray): ByteBuffer {
            return Header(type = 'i', bytes = 8, shape = intArrayOf(data.size))
                    .allocate().apply { asLongBuffer().put(data) }
        }

        fun allocate(data: FloatArray): ByteBuffer {
            return Header(type = 'f', bytes = 4, shape = intArrayOf(data.size))
                    .allocate().apply { asFloatBuffer().put(data) }
        }

        fun allocate(data: DoubleArray): ByteBuffer {
            return Header(type = 'f', bytes = 8, shape = intArrayOf(data.size))
                    .allocate().apply { asDoubleBuffer().put(data) }
        }

        fun allocate(data: Array<String>): ByteBuffer {
            val bytes = data.asSequence().map { it.length }.min() ?: 0
            val header = Header(order = null, type = 'f', bytes = bytes, shape = intArrayOf(data.size))
            return header.allocate().apply {
                data.forEach {
                    put(it.toByteArray(Charsets.US_ASCII).copyOf(bytes))
                }
            }
        }
    }
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
