[0.3.0](../../index.md) / [org.jetbrains.bio.npy](../index.md) / [NpyFile](index.md) / [write](.)

# write

`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: BooleanArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L207)

Writes an array in NPY format to a given path.

`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: ByteArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L213)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: ShortArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L219)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: IntArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L225)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: LongArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L231)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: FloatArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L237)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: DoubleArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L243)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: Array<String>, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L249)