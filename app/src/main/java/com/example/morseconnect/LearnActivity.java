package com.example.morseconnect;

import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.*;

public class LearnActivity extends AppCompatActivity {

    Spinner spinnerCategory, spinnerItem;
    TextView txtLetter, txtMorse;
    Button btnPlay;
    View flashIndicator;

    Map<String, Map<String, String>> morseData;
    Map<String, String> currentMap;

    AudioTrack audioTrack;
    CameraManager cameraManager;
    String cameraId;

    Handler handler = new Handler();

    int wpm = 20;
    int frequency = 600;
    float volume = 0.8f;
    int unitMs = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerItem = findViewById(R.id.spinnerLetters);

        txtLetter = findViewById(R.id.txtLetter);
        txtMorse = findViewById(R.id.txtMorse);
        btnPlay = findViewById(R.id.btnPlay);
        flashIndicator = findViewById(R.id.flashIndicator);

        morseData = MorseDatabase.getCategorizedMorse();
        loadSettings();

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception ignored) {}

        // CATEGORY SPINNER
        ArrayAdapter<String> catAdapter = createSpinnerAdapter(
                new ArrayList<>(morseData.keySet())
        );

        spinnerCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String category = spinnerCategory.getSelectedItem().toString();
                currentMap = morseData.get(category);

                ArrayAdapter<String> itemAdapter = createSpinnerAdapter(
                        new ArrayList<>(currentMap.keySet())
                );

                spinnerItem.setAdapter(itemAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ITEM SPINNER
        spinnerItem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String key = spinnerItem.getSelectedItem().toString();
                txtLetter.setText(key);
                txtMorse.setText(currentMap.get(key));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPlay.setOnClickListener(v -> {
            String code = currentMap.get(txtLetter.getText().toString());
            playMorse(code);
        });
    }

    // SPINNER ADAPTER
    // Makes both the selected text and dropdown options white.
    private ArrayAdapter<String> createSpinnerAdapter(ArrayList<String> items) {

        return new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {

                TextView textView = (TextView) super.getView(position, convertView, parent);

                // Selected Spinner text
                textView.setTextColor(0xFFFFFFFF);
                textView.setTextSize(15);

                return textView;
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent
            ) {

                TextView textView = (TextView) super.getDropDownView(
                        position,
                        convertView,
                        parent
                );

                // Dropdown option text
                textView.setTextColor(0xFFFFFFFF);
                textView.setTextSize(15);

                return textView;
            }
        };
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("MorseSettings", MODE_PRIVATE);
        frequency = prefs.getInt("frequency", 600);
        int volInt = prefs.getInt("volume", 80);
        volume = volInt / 100f;
        wpm = prefs.getInt("speed", 20);
        unitMs = 1200 / wpm;
    }

    private void playMorse(String code) {
        long delay = 0;

        for (char c : code.toCharArray()) {
            if (c == '.') {
                scheduleSignal(delay, unitMs);
                delay += unitMs * 2;
            } else if (c == '-') {
                scheduleSignal(delay, unitMs * 3);
                delay += unitMs * 4;
            } else if (c == ' ') {
                delay += unitMs * 2;
            }
        }
    }

    private void scheduleSignal(long start, int duration) {
        handler.postDelayed(this::startSignal, start);
        handler.postDelayed(this::stopSignal, start + duration);
    }

    private void startSignal() {
        stopSignal();

        int sampleRate = 44100;
        double period = (double) sampleRate / frequency;
        int numSamples = (int) (Math.round(period) * 50);
        if (numSamples == 0) numSamples = 441;

        double[] sample = new double[numSamples];
        byte[] generatedSnd = new byte[2 * numSamples];

        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / period);
        }

        int idx = 0;

        for (double dVal : sample) {
            short val = (short) (dVal * 32767 * volume);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC
        );

        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.setLoopPoints(0, numSamples, -1);
        audioTrack.play();

        try {
            cameraManager.setTorchMode(cameraId, true);
        } catch (Exception ignored) {}

        flashIndicator.setBackgroundResource(R.drawable.indicator_on);
    }

    private void stopSignal() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (Exception ignored) {}

        flashIndicator.setBackgroundResource(R.drawable.indicator_off);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (audioTrack != null) {
            audioTrack.release();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}