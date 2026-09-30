package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class QuizModeActivity extends AppCompatActivity {

    Button btnEasy, btnMedium, btnHard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_mode);

        // Custom back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnEasy = findViewById(R.id.btnEasy);
        btnMedium = findViewById(R.id.btnMedium);
        btnHard = findViewById(R.id.btnHard);

        btnEasy.setOnClickListener(v -> startQuiz("easy"));
        btnMedium.setOnClickListener(v -> startQuiz("medium"));
        btnHard.setOnClickListener(v -> startQuiz("hard"));
    }

    private void startQuiz(String mode) {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}