package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class DailyChallengeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_challenge);

        TextView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnStartDaily = findViewById(R.id.btnStartDaily);

        btnBack.setOnClickListener(v -> finish());

        btnStartDaily.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DailyChallengeActivity.this,
                    QuizActivity.class
            );
            intent.putExtra("mode", "easy");
            startActivity(intent);
        });
    }
}