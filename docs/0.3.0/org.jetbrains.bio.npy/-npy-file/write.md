[0.3.0](../../index.md) / [org.jetbrains.bio.npy](../index.md) / [NpyFile](index.md) / [write](.)

# write

`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: BooleanArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L208)

Writes an array in NPY format to a given path.

`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: ByteArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L214)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: ShortArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L220)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: IntArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L226)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: LongArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L232)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: FloatArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L238)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: DoubleArray, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L244)
`@JvmOverloads @JvmStatic fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: Array<String>, shape: IntArray = intArrayOf(data.size)): Unit` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L250)