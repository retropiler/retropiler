package io.github.retropiler.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.runner.AndroidJUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class OptionalTest {

    @Test
    public void optionalGet() throws Exception {
        Optional<String> s = Optional.of("foo");

        assertThat(s.get(), is("foo"));
    }

    @Test
    public void optionalIfPresent() throws Exception {
        Optional<String> s = Optional.of("foo");

        List<String> result = new ArrayList<>();

        s.ifPresent(result::add);

        assertThat(result, contains("foo"));
    }

    @Test
    public void optionalMap() throws Exception {
        Optional<String> s = Optional.of("foo");

        s.map(String::toUpperCase).ifPresent(result -> {
            assertThat(result, is("FOO"));
        });
    }
}
