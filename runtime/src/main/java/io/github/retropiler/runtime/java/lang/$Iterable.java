package io.github.retropiler.runtime.java.lang;

import io.github.retropiler.annotation.RetroMixin;
import io.github.retropiler.runtime.java.util.$Objects;
import io.github.retropiler.runtime.java.util.function.$Consumer;

@RetroMixin(Iterable.class)
public class $Iterable {

    public static <T> void forEach(Iterable<T> iterable, $Consumer<T> action) {
        $Objects.requireNonNull(action);
        for (T t : iterable) {
            action.accept(t);
        }
    }
}
