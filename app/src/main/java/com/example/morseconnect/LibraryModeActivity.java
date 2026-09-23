package com.example.morseconnect;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LibraryModeActivity extends AppCompatActivity {

    Button btnLearn, btnPractice;
    MediaPlayer clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_mode);

        //  BACK BUTTON
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        btnLearn = findViewById(R.id.btnLearn);
        btnPractice = findViewById(R.id.btnPractice);

        clickSound = MediaPlayer.create(this, R.raw.beep);

        btnLearn.setOnClickListener(v ->
                startActivity(new Intent(this, LearnActivity.class))
        );

        btnPractice.setOnClickListener(v ->
                startActivity(new Intent(this, PracticeActivity.class))
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}