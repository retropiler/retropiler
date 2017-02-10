package io.github.retropiler.runtime;


import java.util.function.Consumer;

import io.github.retropiler.annotation.RetroClass;

@FunctionalInterface
@RetroClass(Consumer.class)
public interface JavaUtilFunctionConsumer<T> {

    void accept(T t);

}
