package io.github.retropiler.runtime;

import io.github.retropiler.annotation.RetroMixin;

@RetroMixin(Iterable.class)
public class JavaUtilIterable {

    public static <T> void forEach(Iterable<T> iterable, JavaUtilFunctionConsumer<T> action) {
        JavaUtilObjects.requireNonNull(action);
        for (T t : iterable) {
            action.accept(t);
        }
    }
}
