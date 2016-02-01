npy [![Build Status](https://travis-ci.org/JetBrains-Research/npy.svg?branch=master)](https://travis-ci.org/JetBrains-Research/npy)
===

`npy` allows to read and write files in [NPY] [npy] and [NPZ] [npy] formats
on the JVM.

Installation
------------

The latest version of `npy` is available on [jCenter] [jcenter]. If you're using
Gradle just add the following to your `build.gradle`:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'org.jetbrains.bio:npy:0.1.0'
}

```

[jcenter]: https://bintray.com/bintray/jcenter
[npy]: http://docs.scipy.org/doc/numpy-dev/neps/npy-format.html
