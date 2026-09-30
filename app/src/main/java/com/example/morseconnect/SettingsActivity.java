package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private TextView lblFrequency;
    private TextView lblVolume;
    private TextView lblSpeed;

    private SeekBar seekFrequency;
    private SeekBar seekVolume;
    private SeekBar seekSpeed;

    private Button btnLogout;
    private View btnBack;
    private View cardFaq;
    private View cardAbout;
    private View cardHowItWorks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Hide the ActionBar to avoid a second back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Back button
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Information cards
        cardFaq = findViewById(R.id.cardFaq);
        cardAbout = findViewById(R.id.cardAbout);
        cardHowItWorks = findViewById(R.id.cardHowItWorks);

        cardFaq.setOnClickListener(v -> showFaq());
        cardAbout.setOnClickListener(v -> showAbout());
        cardHowItWorks.setOnClickListener(v -> showHowItWorks());

        // Preferences
        sharedPreferences = getSharedPreferences(
                "MorseSettings",
                MODE_PRIVATE
        );

        // Settings views
        lblFrequency = findViewById(R.id.lblFrequency);
        seekFrequency = findViewById(R.id.seekFrequency);

        lblVolume = findViewById(R.id.lblVolume);
        seekVolume = findViewById(R.id.seekVolume);

        lblSpeed = findViewById(R.id.lblSpeed);
        seekSpeed = findViewById(R.id.seekSpeed);

        btnLogout = findViewById(R.id.btnLogout);

        // Load saved values
        int freq = sharedPreferences.getInt("frequency", 600);
        int vol = sharedPreferences.getInt("volume", 80);
        int speed = sharedPreferences.getInt("speed", 20);

        freq = Math.max(200, Math.min(freq, 1200));
        vol = Math.max(0, Math.min(vol, 100));
        speed = Math.max(0, Math.min(speed, 50));

        seekFrequency.setProgress(freq);
        lblFrequency.setText(
                getString(R.string.label_frequency, freq)
        );

        seekVolume.setProgress(vol);
        lblVolume.setText(
                getString(R.string.label_volume, vol)
        );

        seekSpeed.setProgress(speed);
        lblSpeed.setText(
                getString(R.string.label_speed, speed)
        );

        // Frequency
        seekFrequency.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (progress < 200) {
                            progress = 200;
                            seekBar.setProgress(progress);
                        }

                        lblFrequency.setText(
                                getString(
                                        R.string.label_frequency,
                                        progress
                                )
                        );

                        sharedPreferences.edit()
                                .putInt("frequency", progress)
                                .apply();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );

        // Volume
        seekVolume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        lblVolume.setText(
                                getString(
                                        R.string.label_volume,
                                        progress
                                )
                        );

                        sharedPreferences.edit()
                                .putInt("volume", progress)
                                .apply();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );

        // Morse speed
        seekSpeed.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        lblSpeed.setText(
                                getString(
                                        R.string.label_speed,
                                        progress
                                )
                        );

                        sharedPreferences.edit()
                                .putInt("speed", progress)
                                .apply();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );

        // Logout
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SettingsActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }

    private void showFaq() {
        String message =
                "1. What is Morse code?\n" +
                        "Morse code represents letters and numbers " +
                        "using dots and dashes.\n\n" +

                        "2. Do I need to know Morse code already?\n" +
                        "No. You can start with the Library Mode " +
                        "and learn the basics.\n\n" +

                        "3. What is the difference between Training " +
                        "and Challenge Mode?\n" +
                        "Training is for learning at your own pace. " +
                        "Challenge Mode is for testing your skills.\n\n" +

                        "4. Can I change the sound?\n" +
                        "Yes. Adjust frequency, volume and speed " +
                        "in the Transmission section.";

        new AlertDialog.Builder(this)
                .setTitle("Frequently Asked Questions")
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showAbout() {
        String message =
                "MorseConnect\n\n" +
                        "MorseConnect is a learning app designed " +
                        "to help users understand and practice " +
                        "Morse code.\n\n" +
                        "Explore the Library, practice with Quiz " +
                        "Mode, and test your skills with challenges.\n\n" +
                        "Version 1.0";

        new AlertDialog.Builder(this)
                .setTitle("About MorseConnect")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showHowItWorks() {
        String message =
                "1. Library Mode\n" +
                        "Study Morse code letters, numbers and " +
                        "their dot-and-dash patterns.\n\n" +

                        "2. Quiz Mode\n" +
                        "Answer questions to check what you have " +
                        "learned and practice recognizing signals.\n\n" +

                        "3. Daily Challenge\n" +
                        "Try the daily activity to keep practicing.\n\n" +

                        "4. Challenge Mode\n" +
                        "Test your signal skills in a more " +
                        "challenging activity.\n\n" +

                        "Tip: Start with the Library if you are " +
                        "new to Morse code, then practice regularly.";

        new AlertDialog.Builder(this)
                .setTitle("How MorseConnect Works")
                .setMessage(message)
                .setPositiveButton("Let's go", null)
                .show();
    }
}