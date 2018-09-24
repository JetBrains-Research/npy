npy
===
Tests: Linux [![Tests Status](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Epigenome_Tools_Npy)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=Epigenome_Tools_Npy&guest=1)
Windows [![Build Status](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Epigenome_Tools_Npy_NpyWindows)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=Epigenome_Tools_Npy_NpyWindows&guest=1)

`npy` allows to read and write files in [NPY] [npy] and [NPZ] [npy] formats
on the JVM.

[npy]: http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html

Installation
------------

The latest version of `npy` is available on [jCenter] [jcenter]. If you're using
Gradle just add the following to your `build.gradle`:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'org.jetbrains.bio:npy:0.3.3'
}

```

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

Building from source
--------------------

The build process is as simple as

```bash
$ ./gradlew assemble
```

Testing
-------

No extra configuration is required for running the tests from Gradle

```bash
$ ./gradlew test
```

However, some tests require Python and NumPy to run and will be skipped
unless you have these.

Publishing
----------

You can publish a new release with a one-liner

```bash
./gradlew clean assemble test generatePomFileForMavenJavaPublication bintrayUpload
```

Make sure to set Bintray credentials (see API key section
[here](https://bintray.com/profile/edit)) in `$HOME/.gradle/gradle.properties`.

```
$ cat $HOME/.gradle/gradle.properties
bintrayUser=CHANGEME
bintrayKey=CHANGEME
```
