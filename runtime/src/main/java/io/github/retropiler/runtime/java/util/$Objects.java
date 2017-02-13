package io.github.retropiler.runtime.java.util;

import java.util.Objects;

import io.github.retropiler.annotation.RetroClass;

@RetroClass(Objects.class)
public class $Objects {

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

}
