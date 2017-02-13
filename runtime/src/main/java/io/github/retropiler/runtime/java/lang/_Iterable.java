package io.github.retropiler.runtime.java.lang;

import io.github.retropiler.annotation.RetroMixin;
import io.github.retropiler.runtime.java.util._Objects;
import io.github.retropiler.runtime.java.util.function._Consumer;

@RetroMixin(Iterable.class)
public class _Iterable {

    public static <T> void forEach(Iterable<T> iterable, _Consumer<T> action) {
        _Objects.requireNonNull(action);
        for (T t : iterable) {
            action.accept(t);
        }
    }
}
