package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnLibrary, btnQuiz, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Remove old onBackPressed override → use dispatcher instead
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                //  Default behavior (go back / exit app)
                finish();
            }
        });

        //  Hide back button in main (dashboard)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // ADMIN CHECK
        String role = getIntent().getStringExtra("role");
        if ("admin".equals(role)) {
            Toast.makeText(this, "Admin Mode", Toast.LENGTH_SHORT).show();
        }

        //  CONNECT BUTTONS
        btnLibrary = findViewById(R.id.btnLibrary);
        btnQuiz = findViewById(R.id.btnQuiz);
        btnSettings = findViewById(R.id.btnSettings);

        btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LibraryModeActivity.class))
        );

        btnQuiz.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, QuizModeActivity.class))
        );

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class))
        );
    }
}