# Retropiler

This is an experimental project to "retropile" Java8 standard class library to Android before 7.0.


```java
import java.util.Optional;

Optional<String> optStr = Optional.of("foo");

assertThat(optStr.get(), is("foo")); // it works!
```

The basic idea is that replacing Java8-specifc classes / methods to the bundled version of them
byte-weaving.

That is, the above code is converted into:

```java
import io.github.retropiler.runtime.java.util._Optional;

_Optional<String> optStr = _Optional.of("foo");

assertThat(optStr.get(), is("foo")); // it works!
```

It just works!
