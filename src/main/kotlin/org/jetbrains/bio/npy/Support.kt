package org.jetbrains.bio.npy

/** A marker function for "impossible" `when` branches. */
@Suppress("nothing_to_inline")
internal inline fun impossible(): Nothing = throw IllegalStateException()

/** Returns a product of array values. */
@Suppress("nothing_to_inline")
internal inline fun IntArray.product() = this.reduce { a, b -> a * b}