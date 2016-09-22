[0.3.1](../../../index.md) / [org.jetbrains.bio.npy](../../index.md) / [NpzFile](../index.md) / [Reader](.)

# Reader

`data class Reader : `[`Closeable`](http://docs.oracle.com/javase/6/docs/api/java/io/Closeable.html)`, `[`AutoCloseable`](http://docs.oracle.com/javase/6/docs/api/java/lang/AutoCloseable.html) [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.1/src/main/kotlin/org/jetbrains/bio/npy/Npz.kt#L33)

A reader for NPZ format.

**Since**
0.2.0

### Properties

| Name | Summary |
|---|---|
| [path](path.md) | `val path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html) |

### Functions

| Name | Summary |
|---|---|
| [close](close.md) | `fun close(): Unit` |
| [get](get.md) | `operator fun get(name: String): `[`NpyArray`](../../-npy-array/index.md)<br>Returns an array for a given name. |
| [introspect](introspect.md) | `fun introspect(): <ERROR CLASS>`<br>Returns a mapping from array names to the corresponding
scalar types in Java. |
