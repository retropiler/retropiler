package io.github.retropiler.test;


import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.runner.AndroidJUnit4;

import java.util.function.Function;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FunctionTest {

    @Test
    public void interfaceStaticMethods() throws Exception {
        Function<String, String> func = Function.identity();
        assertThat(func.apply("foo"), is("foo"));
    }

    @Test
    public void defaultMethods() throws Exception {
        Function<String, String> func = ((Function<String, String>) String::toUpperCase).andThen(String::trim);
        assertThat(func.apply(" foo "), is("FOO"));
    }
}
