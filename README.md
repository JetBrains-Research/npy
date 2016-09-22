npy [![Build Status](https://travis-ci.org/JetBrains-Research/npy.svg?branch=master)](https://travis-ci.org/JetBrains-Research/npy) [![Build status](https://ci.appveyor.com/api/projects/status/065b7jsxfxao374q?svg=true)](https://ci.appveyor.com/project/superbobry/npy)
===

`npy` allows to read and write files in [NPY] [npy] and [NPZ] [npy] formats
on the JVM.

Examples
--------

### NPY

```kotlin
val values = intArrayOf(1, 2, 3, 4, 5, 6)
val path = Paths.get("sample.npy")
NpyFile.write(path, values, shape = intArrayOf(2, 3))

println(NpyFile.read(path))
// => NpyArray{data=[1, 2, 3, 4, 5, 6], shape=[2, 3]}
```

### NPZ

```kotlin
val values1 = intArrayOf(1, 2, 3, 4, 5, 6)
val values2 = booleanArrayOf(true, false)
val path = Paths.get("sample.npz")

NpzFile.write(path).use {
    it.write("xs", values1, shape = intArrayOf(2, 3))
    it.write("mask", values2)
}

NpzFile.read(path).use {
    println(it.introspect())
    // => [NpzEntry{name=xs, type=int, shape=[2, 3]},
    //     NpzEntry{name=mask, type=boolean, shape=[2]}]

    println("xs   = ${it["xs"]}")
    println("mask = ${it["mask"]}")
    // => xs   = NpyArray{data=[1, 2, 3, 4, 5, 6], shape=[2, 3]}
    //    mask = NpyArray{data=[true, false], shape=[2]}
}
```

Installation
------------

The latest version of `npy` is available on [jCenter] [jcenter]. If you're using
Gradle just add the following to your `build.gradle`:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'org.jetbrains.bio:npy:0.3.1'
}

```

Limitations
-----------

The implementation is rather minimal at the moment. Specifically it does
**not** support the following types:

* unsigned integral types (treated as signed),
* bit field,
* complex,
* object,
* Unicode
* void*
* intersections aka types for structured arrays.

[jcenter]: https://bintray.com/bintray/jcenter
[npy]: http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html
