# Retropiler [![CircleCI](https://circleci.com/gh/retropiler/retropiler.svg?style=svg)](https://circleci.com/gh/retropiler/retropiler) [ ![Download](https://api.bintray.com/packages/retropiler/maven/retropiler-gradle-plugin/images/download.svg) ](https://bintray.com/retropiler/maven/retropiler-gradle-plugin/_latestVersion)

"Java8 support" in Android is sometimes misunderstood because it includes a few independent issues.

Java8 Language Feature is usually syntactic one, for example lambda expressions or default methods; in Android, this is solved by tools like `retrolambda` or `desugar`.

Java8 API, or Standard Library, is runtime library on the platform. The support of Standard Library in Android is more difficult than Language Feature, because dex, Dalvik executable file format, does not allow bundle Standard Library in APK. In fact, it was considered impossible -- before this project shows its possibility.

Retropiler deals with the latter: it makes dex to bundle Java8 standard library by replacing
its references to the original ones.

For example, the following code works on devices with Android API level 15 after processing by retropiler:

```java
import java.util.Optional;

Optional<String> optStr = Optional.of("foo");

assertThat(optStr.get(), is("foo")); // it works!
```

Here is the magic.

The basic idea is that replacing Java8-specifc classes / methods to the bundled version of them
with bytecode weaving.

That is, the above code is transformed into:

```java
import io.github.retropiler.runtime.java.util._Optional;

_Optional<String> optStr = _Optional.of("foo");

assertThat(optStr.get(), is("foo")); // it works!
```

It can work even on Android API 15.

## Installation

```groovy:build.gradle
buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath 'io.github.retropiler:retropiler-gradle-plugin:0.0.5'
    }
}

apply plugin: 'io.github.retropiler'
```

## Supported Classes

### `Iterable#forEach()`

```java
Arrays.asList("foo", "bar").forEach(item -> {
    Log.d("XXX", item);
});
```

### `java.util.Optional`

```java
Optional<String> optStr = Optional.of("baz");
optStr.ifPresent(str -> {
    Log.d("XXX", str);
});
```

### Part of `java.util.function` package

Not all the functions are tested yet.

## Customizing Runtime Class Mapping

You can customize mapping of stndard class to alternative runtime class:

```groovy:build.gradle
apply plugin: 'io.github.retropiler'

retropiler {
    // the default runtime package
    runtimePackage "io.github.retropiler.runtime"

    // the default mapping function
    mapClassName { ext, ctClass ->
        // e.g. map java.util.Optional to io.github.retropiler.runtime.java.util._Optional
        "${ext.runtimePackage}.${ctClass.packageName}._${ctClass.simpleName}"
    }
}
```

## Methods Count

Retropiler depends on Retrolambda to transform lambda expressions to anonymous classes.

The methods defined in `runtime/` are here:

[io.github.retropiler:retropiler-runtime:0.0.5](http://www.methodscount.com/?lib=io.github.retropiler%3Aretropiler-runtime%3A0.0.5)

## Caveats

* This project is incubating so use of it in production is not recommended.
* The gradle plugin automatically applies `retrolambda`
* The gradle plugin does not support Android SDK's desugar (bundled in the build tools for Android O)
  * Will support desugar after its stable release

## See Also

* [Use Java 8 language features \| Android Studio](https://developer.android.com/studio/preview/features/java8-support.html) explains the "desugar" process that, for example, transforms lambda expressions to anonymous class expressions
* [Retrolambda](https://github.com/orfjackal/retrolambda)

## Release Engineering

There are three modules to publish:

* `plugin/` - Gradle plugin
* `runtime/` - runtime library
* `annotations/` - annotation library

You can publish them by:

```shell
make publish
```

with properties in `~/.gradle/gradle.properties`:

```gradle.properties
bintrayUser=$user
bintrayKey=$key
```

## Authors and Contributors

FUJI Goro ([gfx](https://github.com/gfx)).

And contributors are listed here: [Contributors](https://github.com/retropiler/retropiler/graphs/contributors)

The class library in [runtime](https://github.com/retropiler/retropiler/tree/master/runtime) module comes from Open JDK via AOSP.

## Licenses

The Java source files in `runtime/` module which were copied from AOSP, are licensed under GPL v2+CE as described in each file.

Other modules except for `runtime/` are licensed under Apache License 2.0:

Copyright (c) 2017 The Retropiler Project.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
