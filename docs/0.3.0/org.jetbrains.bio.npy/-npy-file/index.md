[0.3.0](../../index.md) / [org.jetbrains.bio.npy](../index.md) / [NpyFile](.)

# NpyFile

`object NpyFile` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L35)

A file in NPY format.

Currently unsupported types:

* unsigned integral types (treated as signed)
* bit field,
* complex,
* object,
* Unicode
* void*
* intersections aka types for structured arrays.

See http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html

Example:

``` kotlin
val values = intArrayOf(1, 2, 3, 4, 5, 6)
val path = Paths.get("sample.npy")
NpyFile.write(path, values, shape = intArrayOf(2, 3))
println(NpyFile.read(path))
// =&gt; NpyArray{data=[1, 2, 3, 4, 5, 6], shape=[2, 3]}
```

### Functions

| Name | Summary |
|---|---|
| [read](read.md) | `fun read(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`): `[`NpyArray`](../-npy-array/index.md)<br>Reads an array in NPY format from a given path. |
| [write](write.md) | `fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: BooleanArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>Writes an array in NPY format to a given path.`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: ByteArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: ShortArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: IntArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: LongArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: FloatArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: DoubleArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, data: Array<String>, shape: IntArray = intArrayOf(data.size)): Unit` |
