package com.example.morseconnect;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DailyChallengeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("Daily Challenge");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText(
                "Complete today's Morse code warm-up!\n\n" +
                        "Practice daily and build your streak."
        );
        description.setTextSize(16);
        description.setTextColor(Color.LTGRAY);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 24, 0, 32);

        Button backButton = new Button(this);
        backButton.setText("Back to Home");
        backButton.setOnClickListener(v -> finish());

        layout.addView(title);
        layout.addView(description);
        layout.addView(backButton);

        setContentView(layout);
    }
}