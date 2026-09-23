package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    Button btnLogout;
    SeekBar seekFrequency, seekVolume, seekSpeed;
    TextView lblFrequency, lblVolume, lblSpeed;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("MorseSettings", MODE_PRIVATE);

        // Back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        lblFrequency = findViewById(R.id.lblFrequency);
        seekFrequency = findViewById(R.id.seekFrequency);
        lblVolume = findViewById(R.id.lblVolume);
        seekVolume = findViewById(R.id.seekVolume);
        lblSpeed = findViewById(R.id.lblSpeed);
        seekSpeed = findViewById(R.id.seekSpeed);
        btnLogout = findViewById(R.id.btnLogout);

        // Load saved settings
        int freq = sharedPreferences.getInt("frequency", 600);
        int vol = sharedPreferences.getInt("volume", 80);
        int speed = sharedPreferences.getInt("speed", 20);

        seekFrequency.setProgress(freq);
        lblFrequency.setText(getString(R.string.label_frequency, freq));

        seekVolume.setProgress(vol);
        lblVolume.setText(getString(R.string.label_volume, vol));

        seekSpeed.setProgress(speed);
        lblSpeed.setText(getString(R.string.label_speed, speed));

        seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 200) progress = 200; // Min frequency
                lblFrequency.setText(getString(R.string.label_frequency, progress));
                sharedPreferences.edit().putInt("frequency", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblVolume.setText(getString(R.string.label_volume, progress));
                sharedPreferences.edit().putInt("volume", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 5) progress = 5; // Min speed
                lblSpeed.setText(getString(R.string.label_speed, progress));
                sharedPreferences.edit().putInt("speed", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // If you use login session, clear it here
                // getSharedPreferences("user", MODE_PRIVATE).edit().clear().apply();

                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}