package io.github.retropiler.runtime;

import java.util.Objects;

import io.github.retropiler.annotation.RetroClass;

/**
 * @see java.util.Objects
 */
@RetroClass(Objects.class)
public class JavaUtilObjects {

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

}
