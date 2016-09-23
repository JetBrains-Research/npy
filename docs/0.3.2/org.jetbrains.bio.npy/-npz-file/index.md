[0.3.2](../../index.md) / [org.jetbrains.bio.npy](../index.md) / [NpzFile](.)

# NpzFile

`object NpzFile` [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.2/src/main/kotlin/org/jetbrains/bio/npy/Npz.kt#L27)

A ZIP file where each individual file is in NPY format.

By convention each file has `.npy` extension, however, the API
doesnt expose it. So for instance the array named "X" will be
accessibly via "X" and **not** "X.npy".

Example:

``` kotlin
val values1 = intArrayOf(1, 2, 3, 4, 5, 6)
val values2 = booleanArrayOf(true, false)
val path = Paths.get("sample.npz")
NpzFile.write(path).use {
    it.write("xs", values1, shape = intArrayOf(2, 3))
    it.write("mask", values2)
}
NpzFile.read(path).use {
    println(it.introspect())
    // =&gt; [NpzEntry{name=xs, type=int, shape=[2, 3]},
    //     NpzEntry{name=mask, type=boolean, shape=[2]}]
    println("xs   = ${it["xs"]}")
    println("mask = ${it["mask"]}")
    // =&gt; xs   = NpyArray{data=[1, 2, 3, 4, 5, 6], shape=[2, 3]}
    //    mask = NpyArray{data=[true, false], shape=[2]}
}
```

### Types

| Name | Summary |
|---|---|
| [Reader](-reader/index.md) | `data class Reader : `[`Closeable`](http://docs.oracle.com/javase/6/docs/api/java/io/Closeable.html)`, `[`AutoCloseable`](http://docs.oracle.com/javase/6/docs/api/java/lang/AutoCloseable.html)<br>A reader for NPZ format. |
| [Writer](-writer/index.md) | `data class Writer : `[`Closeable`](http://docs.oracle.com/javase/6/docs/api/java/io/Closeable.html)`, `[`AutoCloseable`](http://docs.oracle.com/javase/6/docs/api/java/lang/AutoCloseable.html)<br>A writer for NPZ format. |

### Functions

| Name | Summary |
|---|---|
| [read](read.md) | `fun read(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`): `[`Reader`](-reader/index.md)<br>Opens an NPZ file at [path](read.md#org.jetbrains.bio.npy.NpzFile$read(java.nio.file.Path)/path) for reading. |
| [write](write.md) | `fun write(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`, compressed: Boolean = false): `[`Writer`](-writer/index.md)<br>Opens an NPZ file at [path](write.md#org.jetbrains.bio.npy.NpzFile$write(java.nio.file.Path, kotlin.Boolean)/path) for writing. |
