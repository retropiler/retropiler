package io.github.retropiler.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.annotation.SuppressLint;
import android.support.test.runner.AndroidJUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.github.retropiler.runtime.java.util._Optional;
import io.github.retropiler.runtime.java.util.function._Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
@SuppressLint("NewApi")
public class OptionalTest {

    @Test
    public void optionalGet() throws Exception {
        Optional<String> s = Optional.of("foo");

        assertThat(s.get(), is("foo"));
    }

    @Test
    public void optionalIfPresent() throws Exception {
        // FIXME it causes dx(1) to crash
        Optional<String> s = Optional.of("foo");

        List<String> result = new ArrayList<>();

        s.ifPresent(new Consumer<String>() {
            @Override
            public void accept(String s) {
                result.add(s);
            }
        });

        assertThat(result, contains("foo"));
    }

    @Test
    public void optionalIfPresent2() throws Exception {
        _Optional<String> s = _Optional.of("foo");

        List<String> result = new ArrayList<>();

        s.ifPresent(new _Consumer<String>() {
            @Override
            public void accept(String s) {
                result.add(s);
            }
        });

        assertThat(result, contains("foo"));
    }

}
