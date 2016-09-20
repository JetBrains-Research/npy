[0.3.0](../../index.md) / [org.jetbrains.bio.npy](../index.md) / [NpyFile](index.md) / [&lt;init&gt;](.)

# &lt;init&gt;

`NpyFile()`

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

