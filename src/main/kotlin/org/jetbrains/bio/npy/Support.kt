package org.jetbrains.bio.npy

/** A marker function for "impossible" `when` branches. */
@Suppress("nothing_to_inline")
inline fun impossible(): Nothing = throw IllegalStateException()
