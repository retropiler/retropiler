package io.github.retropiler.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Arrays;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Arrays.asList("foo", "bar").forEach(s -> {
            Log.d("RetropilerExample", s);
        });

        Arrays.asList("foo", "bar").forEach(s -> {
            Log.d("RetropilerExample", MainActivity.this + " " + s);
        });
    }
}
