[0.3.2](../../../index.md) / [org.jetbrains.bio.npy](../../index.md) / [NpzFile](../index.md) / [Reader](index.md) / [get](.)

# get

`operator fun get(name: String, step: Int = 1 shl 18): `[`NpyArray`](../../-npy-array/index.md) [(source)](https://github.com/JetBrains-Research/npy/blob/0.3.2/src/main/kotlin/org/jetbrains/bio/npy/Npz.kt#L76)

Returns an array for a given name.

The caller is responsible for casting the resulting array to an
appropriate type.

### Parameters

`name` - array name.

`step` - amount of bytes to use for the temporary buffer, when
reading the entry. Defaults to 1 &lt;&lt; 18 to mimic NumPy
behaviour.