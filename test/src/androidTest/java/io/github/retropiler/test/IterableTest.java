package io.github.retropiler.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.annotation.SuppressLint;
import android.support.test.runner.AndroidJUnit4;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@SuppressLint("NewApi")
public class IterableTest {

    @Test
    public void useForEach() throws Exception {
        List<String> a = Arrays.asList("foo", "bar");

        a.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println("--> " + s);
            }
        });
    }
}
