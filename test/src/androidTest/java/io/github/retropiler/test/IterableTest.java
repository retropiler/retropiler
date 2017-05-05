package io.github.retropiler.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.runner.AndroidJUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class IterableTest {

    @Test
    public void useForEachWithLambda() throws Exception {
        List<String> a = Arrays.asList("foo", "bar");
        List<String> b = new ArrayList<>();

        a.forEach(b::add);

        assertThat(b, is(a));
    }
}
