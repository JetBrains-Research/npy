package org.jetbrains.bio.npy

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Default buffer size for [ArraySplitBuffer] subclasses. */
private const val DEFAULT_BUFFER_SIZE = 65536

/**
 * A chunked iterator for primitive array types.
 *
 * The maximum chunk size is currently a constant and is defined by
 * [DEFAULT_BUFFER_SIZE].
 *
 * Why? Java does not provide an API for coercing a primitive buffer
 * to a [ByteBuffer] without copying, because a primitive buffer might
 * have a non-native byte ordering. This class implements
 * constant-memory iteration over a primitive array without resorting
 * to primitive buffers.
 *
 * Invariant: buffers produced by the iterator must be consumed
 * **in order**, because their content is invalidated between
 * [Iterator.next] calls.
 *
 * @since 0.3.1
 */
internal abstract class ArraySplitBuffer<T>(
        /** The array. */
        protected val data: T,
        /** Number of elements in the array. */
        private val size: Int,
        /** Byte order for the produced buffers. */
        private val order: ByteOrder) : Sequence<ByteBuffer> {
    abstract val bytes: Int

    /**
     * Populates this buffer using elements from [data].
     *
     * @see ByteBuffer.put
     */
    abstract fun ByteBuffer.fill(data: T, offset: Int, size: Int)

    override fun iterator() = object : Iterator<ByteBuffer> {
        private var offset = 0  // into the [data].
        private var step = DEFAULT_BUFFER_SIZE / bytes
        // Only allocated 'cache' if the [data] is bigger than [step].
        private val cache by lazy {
            ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE).order(order)
        }

        override fun hasNext() = offset < size

        override fun next(): ByteBuffer {
            val available = Math.min(size - offset, step)
            val result = if (available == step) {
                cache.apply { rewind() }
            } else {
                ByteBuffer.allocateDirect(available * bytes).order(order)
            }

            with(result) {
                fill(data, offset, available)
                rewind()
            }

            offset += available
            return result
        }
    }
}

internal class BooleanArraySplitBuffer(data: BooleanArray) :
        ArraySplitBuffer<BooleanArray>(data, data.size, ByteOrder.nativeOrder()) {
    override val bytes: Int get() = 1

    override fun ByteBuffer.fill(data: BooleanArray, offset: Int, size: Int) {
        for (i in offset until offset + size) {
            put(if (data[i]) 1.toByte() else 0.toByte())
        }
    }
}

internal class ShortArraySplitBuffer(data: ShortArray, order: ByteOrder) :
        ArraySplitBuffer<ShortArray>(data, data.size, order) {
    override val bytes: Int get() = java.lang.Short.BYTES

    override fun ByteBuffer.fill(data: ShortArray, offset: Int, size: Int) {
        asShortBuffer().put(data, offset, size)
    }
}

internal class IntArraySplitBuffer(data: IntArray, order: ByteOrder) :
        ArraySplitBuffer<IntArray>(data, data.size, order) {
    override val bytes: Int get() = java.lang.Integer.BYTES

    override fun ByteBuffer.fill(data: IntArray, offset: Int, size: Int) {
        asIntBuffer().put(data, offset, size)
    }
}

internal class LongArraySplitBuffer(data: LongArray, order: ByteOrder) :
        ArraySplitBuffer<LongArray>(data, data.size, order) {
    override val bytes: Int get() = java.lang.Long.BYTES

    override fun ByteBuffer.fill(data: LongArray, offset: Int, size: Int) {
        asLongBuffer().put(data, offset, size)
    }
}

internal class FloatArraySplitBuffer(data: FloatArray, order: ByteOrder) :
        ArraySplitBuffer<FloatArray>(data, data.size, order) {
    override val bytes: Int get() = java.lang.Float.BYTES

    override fun ByteBuffer.fill(data: FloatArray, offset: Int, size: Int) {
        asFloatBuffer().put(data, offset, size)
    }
}

internal class DoubleArraySplitBuffer(data: DoubleArray, order: ByteOrder) :
        ArraySplitBuffer<DoubleArray>(data, data.size, order) {
    override val bytes: Int get() = java.lang.Double.BYTES

    override fun ByteBuffer.fill(data: DoubleArray, offset: Int, size: Int) {
        asDoubleBuffer().put(data, offset, size)
    }
}

internal class StringArraySplitBuffer(data: Array<String>) :
        ArraySplitBuffer<Array<String>>(data, data.size, ByteOrder.nativeOrder()) {
    override val bytes: Int by lazy { data.asSequence().map { it.length }.max() ?: 0 }

    override fun ByteBuffer.fill(data: Array<String>, offset: Int, size: Int) {
        for (i in offset until offset + size) {
            put(data[i].toByteArray(Charsets.US_ASCII).copyOf(bytes))
        }
    }
}
