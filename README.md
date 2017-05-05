# Retropiler [![CircleCI](https://circleci.com/gh/retropiler/retropiler.svg?style=svg)](https://circleci.com/gh/retropiler/retropiler)

This is an experimental project to "retropile", also called as "downpile" in some systems, Java8 standard class libraries to work on Android before 7.0.

That is, the following code works with retropler:

```java
import java.util.Optional;

Optional<String> optStr = Optional.of("foo");

assertThat(optStr.get(), is("foo")); // it works!
```

The basic idea is that replacing Java8-specifc classes / methods to the bundled version of them
byte-weaving.

That is, the above code is transformed into:

```java
import io.github.retropiler.runtime.java.util._Optional;

_Optional<String> optStr = _Optional.of("foo");

assertThat(optStr.get(), is("foo")); // it works!
```

It works even on Android API 15.

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

## Authors and Contributors

FUJI Goro ([gfx](https://github.com/gfx)).

And contributors are listed here: [Contributors](https://github.com/gfx/Android-Orma/graphs/contributors)

## License

Copyright (c) 2017 FUJI Goro (gfx).

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
