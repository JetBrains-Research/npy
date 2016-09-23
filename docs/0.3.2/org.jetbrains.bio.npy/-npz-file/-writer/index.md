[0.3.2](../../../index.md) / [org.jetbrains.bio.npy](../../index.md) / [NpzFile](../index.md) / [Writer](.)

# Writer

`data class Writer : `[`Closeable`](http://docs.oracle.com/javase/6/docs/api/java/io/Closeable.html)`, `[`AutoCloseable`](http://docs.oracle.com/javase/6/docs/api/java/lang/AutoCloseable.html) [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.2/src/main/kotlin/org/jetbrains/bio/npy/Npz.kt#L114)

A writer for NPZ format.

The implementation uses a temporary [ByteBuffer](http://docs.oracle.com/javase/6/docs/api/java/nio/ByteBuffer.html) to store the
serialized array prior to archiving. Thus each [write](write.md) call
requires N extra bytes of memory for an array of N bytes.

### Properties

| Name | Summary |
|---|---|
| [compressed](compressed.md) | `val compressed: Boolean` |
| [path](path.md) | `val path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html) |

### Functions

| Name | Summary |
|---|---|
| [close](close.md) | `fun close(): Unit` |
| [write](write.md) | `fun write(name: String, data: BooleanArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(name: String, data: ByteArray, shape: IntArray = intArrayOf(data.size)): Unit`<br>`fun write(name: String, data: ShortArray, shape: IntArray = intArrayOf(data.size), order: `[`ByteOrder`](http://docs.oracle.com/javase/6/docs/api/java/nio/ByteOrder.html)` = ByteOrder.nativeOrder()): Unit`<br>`fun write(name: String, data: IntArray, shape: IntArray = intArrayOf(data.size), order: `[`ByteOrder`](http://docs.oracle.com/javase/6/docs/api/java/nio/ByteOrder.html)` = ByteOrder.nativeOrder()): Unit`<br>`fun write(name: String, data: LongArray, shape: IntArray = intArrayOf(data.size), order: `[`ByteOrder`](http://docs.oracle.com/javase/6/docs/api/java/nio/ByteOrder.html)` = ByteOrder.nativeOrder()): Unit`<br>`fun write(name: String, data: FloatArray, shape: IntArray = intArrayOf(data.size), order: `[`ByteOrder`](http://docs.oracle.com/javase/6/docs/api/java/nio/ByteOrder.html)` = ByteOrder.nativeOrder()): Unit`<br>`fun write(name: String, data: DoubleArray, shape: IntArray = intArrayOf(data.size), order: `[`ByteOrder`](http://docs.oracle.com/javase/6/docs/api/java/nio/ByteOrder.html)` = ByteOrder.nativeOrder()): Unit`<br>`fun write(name: String, data: Array<String>, shape: IntArray = intArrayOf(data.size)): Unit` |
