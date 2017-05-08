/*
 * Copyright (c) 2017 FUJI Goro (gfx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public void optionalEmpty() throws Exception {
        assertThat(Optional.empty().isPresent(), is(false));
    }

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
