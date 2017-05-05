package io.github.retropiler.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Optional;

@SuppressLint("NewApi")
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
}
