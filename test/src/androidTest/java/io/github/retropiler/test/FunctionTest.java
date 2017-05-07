package io.github.retropiler.test;


import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.runner.AndroidJUnit4;

import java.util.function.Function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FunctionTest {

    @Test
    public void interfaceStaticMethods() throws Exception {
        assertThat(Function.<String>identity().apply("foo"), is("foo"));
    }

    //@Test
    public void defaultMethods() throws Exception {
        assertThat(((Function<String, String>) String::toUpperCase).andThen(String::trim).apply(" foo "), is("FOO"));
    }
}
