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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {

    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);
        text.append("\n\n");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Arrays.asList("foo", "bar").forEach(s -> {
            text.append("Iterable#forEach with binding: " + s + "\n");
        });

        Optional<String> optStr = Optional.of("baz");
        optStr.ifPresent(s -> text.append("Optional#ifPresent: " + s));
    }

    void f() {
        // just places it here for the regression: https://github.com/retropiler/retropiler/pull/10
        Runnable task = () -> {};
        task.run();
    }
}
