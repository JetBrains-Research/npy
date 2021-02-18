[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Tests Status](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Epigenome_Tools_Npy)/statusIcon.svg)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=Epigenome_Tools_Npy&guest=1)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.bio/npy.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.bio%22%20AND%20a:%22npy%22)

npy
===

`npy` allows to read and write files in [NPY] [npy] and [NPZ] [npy] formats
on the JVM.

[npy]: http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html

Installation
------------

The latest version of `npy` is available on [Maven Central] [maven-central]. If you're using
Gradle just add the following to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'org.jetbrains.bio:npy:0.3.5'
}

```

With Maven, specify the following in your `pom.xml`:
```xml
<dependency>
  <groupId>org.jetbrains.bio</groupId>
  <artifactId>npy</artifactId>
  <version>0.3.5</version>
</dependency>
```

The previous versions were published on Bintray. They can be downloaded
from [GitHub Releases](https://github.com/JetBrains-Research/npy/releases).

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

[maven-central]: https://search.maven.org/artifact/org.jetbrains.bio/npy/0.3.5/jar

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