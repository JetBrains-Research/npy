package org.jetbrains.bio.npy

import com.google.common.base.CharMatcher
import com.google.common.base.Charsets
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.primitives.Ints
import com.google.common.primitives.Shorts
import java.io.DataInput
import java.io.DataOutput
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

        fun write(output: DataOutput) = with(output) {
            write(MAGIC)
            write(major)
            write(minor)

            val descr = "${order.toChar()}$type$bytes"
            val metaUnpadded =
                    "{'descr': '$descr', 'fortran_order': False, 'shape': (${shape.joinToString(",")}), }\n"

            // According to the spec the total header size should be
            // evenly divisible by 16 for alignment purposes.
            val total = MAGIC.size + 2 + when (major to minor) {
                1 to 0 -> 2
                2 to 0 -> 4
                else -> TODO()
            } + metaUnpadded.length

            val meta = Strings.padEnd(metaUnpadded, total + total % 16, ' ')
                    .toByteArray(Charsets.US_ASCII)

            when (major to minor) {
                1 to 0 -> {
                    check(meta.size <= Short.MAX_VALUE)
                    val (b0, b1) = Shorts.toByteArray(meta.size.toShort())
                    writeByte(b1.toInt())
                    writeByte(b0.toInt())
                }
                2 to 0 -> {
                    val (b0, b1, b2, b3) = Ints.toByteArray(meta.size)
                    writeByte(b3.toInt())
                    writeByte(b2.toInt())
                    writeByte(b1.toInt())
                    writeByte(b0.toInt())
                }
            }

            write(meta)
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
            private val MAGIC = byteArrayOf(0x93.toByte()) + "NUMPY".toByteArray()

            fun read(input: DataInput) = with(input) {
                val buf = ByteArray(6)
                readFully(buf)
                check(Arrays.equals(MAGIC, buf)) { "bad magic: ${String(buf)}" }

                val major = readUnsignedByte()
                val minor = readUnsignedByte()

                // XXX we can't use 'DataInput' here because it's big endian.
                val b0 = readByte()
                val b1 = readByte()
                val size = when (major to minor) {
                    1 to 0 -> Shorts.fromBytes(b1, b0).toInt()
                    2 to 0 -> {
                        val b2 = readByte()
                        val b3 = readByte()
                        Ints.fromBytes(b3, b2, b1, b0)
                    }
                    else -> error("unsupported version: $major.$minor")
                }

                val header = ByteArray(size)
                readFully(header)

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

                Header(major, minor, order = dtype[0].toByteOrder(),
                       type = dtype[1],
                       bytes = dtype.substring(2).toInt(),
                       shape = shape)
            }
        }
    }

    companion object {
        fun read(input: DataInput): Any {
            val header = Header.read(input)
            val (size) = header.shape
            val buf = ByteArray(header.bytes * size).run {
                input.readFully(this)
                ByteBuffer.wrap(this).order(header.order)
            }

            return when (header.type) {
                'b' -> {
                    check(header.bytes == 1)
                    BooleanArray(size) { buf.get() == 1.toByte() }
                }
                'u', 'i' -> when (header.bytes) {
                    1 -> ByteArray(size).apply { buf.get(this) }
                    2 -> ShortArray(size).apply { buf.asShortBuffer().get(this) }
                    4 -> IntArray(size).apply { buf.asIntBuffer().get(this) }
                    8 -> LongArray(size).apply { buf.asLongBuffer().get(this) }
                    else -> error("invalid number of bytes for ${header.type}: ${header.bytes}")
                }
                'f' -> when (header.bytes) {
                    4 -> FloatArray(size).apply { buf.asFloatBuffer().get(this) }
                    8 -> DoubleArray(size).apply { buf.asDoubleBuffer().get(this) }
                    else -> error("invalid number of bytes for ${header.type}: ${header.bytes}")
                }
                'S' -> Array(size) {
                    val s = ByteArray(header.bytes)
                    buf.get(s)
                    String(s, Charsets.US_ASCII)
                }
                else -> error("unsupported type: ${header.type}")
            }
        }

        fun write(output: DataOutput, data: BooleanArray) {
            Header(order = null, type = 'b', bytes = 1, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.writeBoolean(it) }
        }

        fun write(output: DataOutput, data: ByteArray) {
            Header(type = 'i', bytes = 1, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.write(it.toInt()) }
        }

        fun write(output: DataOutput, data: ShortArray) {
            Header(type = 'i', bytes = 2, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.writeShort(it.toInt()) }
        }

        fun write(output: DataOutput, data: IntArray) {
            Header(type = 'i', bytes = 4, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.writeInt(it) }
        }

        fun write(output: DataOutput, data: LongArray) {
            Header(type = 'i', bytes = 8, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.writeLong(it) }
        }

        fun write(output: DataOutput, data: FloatArray) {
            Header(type = 'f', bytes = 4, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.writeFloat(it) }
        }

        fun write(output: DataOutput, data: DoubleArray) {
            Header(type = 'f', bytes = 8, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach { output.writeDouble(it) }
        }

        fun write(output: DataOutput, data: Array<String>) {
            val bytes = data.asSequence().map { it.length }.min() ?: 0
            Header(order = null, type = 'f', bytes = bytes, shape = intArrayOf(data.size))
                    .write(output)

            data.forEach {
                it.toByteArray()
                output.write(it.toByteArray(Charsets.US_ASCII).copyOf(bytes))
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
