[0.3.0](../../index.md) / [org.jetbrains.bio.npy](../index.md) / [NpyFile](index.md) / [read](.)

# read

`@JvmStatic fun read(path: `[`Path`](http://docs.oracle.com/javase/6/docs/api/java/nio/file/Path.html)`): `[`NpyArray`](../-npy-array/index.md) [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.0/src/main/kotlin/org/jetbrains/bio/npy/Npy.kt#L166)

Reads an array in NPY format from a given path.

The caller is responsible for coercing the resulting array to
an appropriate type via [NpyArray](../-npy-array/index.md) methods.

