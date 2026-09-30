package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LibraryModeActivity extends AppCompatActivity {

    private Button btnLearn;
    private Button btnPractice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_mode);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnLearn = findViewById(R.id.btnLearn);
        btnPractice = findViewById(R.id.btnPractice);

        btnLearn.setOnClickListener(v ->
                startActivity(new Intent(
                        LibraryModeActivity.this,
                        LearnActivity.class
                ))
        );

        btnPractice.setOnClickListener(v ->
                startActivity(new Intent(
                        LibraryModeActivity.this,
                        PracticeActivity.class
                ))
        );
    }
}