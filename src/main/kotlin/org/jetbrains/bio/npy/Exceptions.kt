package org.jetbrains.bio.npy

internal fun unexpectedHeaderType(headerType: Char): Nothing =
    throw IllegalStateException("unexpected header type $headerType")

internal fun unexpectedByteNumber(headerType: Char, byteCount: Int): Nothing =
    throw IllegalStateException("unexpected number of bytes $byteCount for header type $headerType")

