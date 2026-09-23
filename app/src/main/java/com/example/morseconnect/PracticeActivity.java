package com.example.morseconnect;

import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.*;

public class PracticeActivity extends AppCompatActivity {

    TextView txtQuestion, txtInput, txtOutput;
    EditText edtInput;
    Button btnCheck, btnReset, btnDelete, btnMorse, btnPlayInput;
    View flashIndicator;

    StringBuilder userInput = new StringBuilder();
    String correctAnswer;

    Map<String, String> morseMap;
    List<String> letters;

    long pressStartTime;

    int wpm = 20;
    int frequency = 600;
    float volume = 0.8f;
    int unitMs = 60;

    AudioTrack audioTrack;
    Handler handler = new Handler();

    CameraManager cameraManager;
    String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        txtQuestion = findViewById(R.id.txtQuestion);
        txtInput = findViewById(R.id.txtInput);
        txtOutput = findViewById(R.id.txtOutput);

        edtInput = findViewById(R.id.edtInput);
        btnPlayInput = findViewById(R.id.btnPlayInput);

        btnCheck = findViewById(R.id.btnCheck);
        btnReset = findViewById(R.id.btnReset);
        btnDelete = findViewById(R.id.btnDelete);
        btnMorse = findViewById(R.id.btnMorse);
        flashIndicator = findViewById(R.id.flashIndicator);

        morseMap = MorseDatabase.getMorseMap();
        letters = new ArrayList<>(morseMap.keySet());
        Collections.sort(letters);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try { cameraId = cameraManager.getCameraIdList()[0]; } catch (Exception ignored) {}

        loadSettings();
        generateQuestion();

        //  TAP / HOLD INPUT
        btnMorse.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    pressStartTime = System.currentTimeMillis();
                    startSignal();
                    return true;

                case MotionEvent.ACTION_UP:
                    stopSignal();

                    long duration = System.currentTimeMillis() - pressStartTime;

                    if (duration < unitMs * 2) {
                        userInput.append(".");
                    } else {
                        userInput.append("-");
                    }

                    txtInput.setText(userInput.toString());
                    return true;
            }
            return false;
        });

        btnCheck.setOnClickListener(v -> checkAnswer());

        btnDelete.setOnClickListener(v -> {
            userInput.setLength(0);
            txtInput.setText("");
        });

        btnReset.setOnClickListener(v -> {
            userInput.setLength(0);
            txtInput.setText("");
            generateQuestion();
        });

        // 🔥 NEW: PLAY USER INPUT TEXT
        btnPlayInput.setOnClickListener(v -> {
            String text = edtInput.getText().toString().toUpperCase();
            String morse = convertToMorse(text);

            txtOutput.setText(morse);
            playMorse(morse);
        });
    }

    // 🔥 TEXT → MORSE
    private String convertToMorse(String text) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c == ' ') {
                result.append("   "); // word gap
            } else {
                String code = morseMap.get(String.valueOf(c));
                if (code != null) {
                    result.append(code).append(" ");
                }
            }
        }

        return result.toString();
    }

    //  PLAY MORSE
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("MorseSettings", MODE_PRIVATE);
        frequency = prefs.getInt("frequency", 600);
        int volInt = prefs.getInt("volume", 80);
        volume = volInt / 100f;
        wpm = prefs.getInt("speed", 20);
        unitMs = 1200 / wpm;
    }

    private void startSignal() {
        stopSignal(); // Ensure previous is stopped
        
        int sampleRate = 44100;
        // Generate a buffer that contains an integer number of cycles to avoid clicks
        double period = (double) sampleRate / frequency;
        int numSamples = (int) (Math.round(period) * 50); // 50 cycles
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

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.setLoopPoints(0, numSamples, -1);
        audioTrack.play();

        try { cameraManager.setTorchMode(cameraId, true); } catch (Exception ignored) {}
        flashIndicator.setBackgroundResource(R.drawable.indicator_on);
    }

    private void stopSignal() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        try { cameraManager.setTorchMode(cameraId, false); } catch (Exception ignored) {}
        flashIndicator.setBackgroundResource(R.drawable.indicator_off);
    }

    private void playMorse(String code) {
        long delay = 0;

        for (char c : code.toCharArray()) {
            if (c == '.') {
                scheduleSignal(delay, unitMs);
                delay += unitMs * 2;
            } else if (c == '-') {
                scheduleSignal(delay, unitMs * 3);
                delay += (unitMs * 3) + unitMs;
            } else if (c == ' ') {
                delay += unitMs * 2;
            }
        }
    }

    private void scheduleSignal(long start, int duration) {
        handler.postDelayed(this::startSignal, start);
        handler.postDelayed(this::stopSignal, start + duration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    private void generateQuestion() {
        Random random = new Random();
        String letter = letters.get(random.nextInt(letters.size()));
        correctAnswer = morseMap.get(letter);
        txtQuestion.setText(letter);
    }

    private void checkAnswer() {
        if (userInput.toString().equals(correctAnswer)) {
            Toast.makeText(this, "✅ Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Correct: " + correctAnswer, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioTrack != null) {
            audioTrack.release();
        }
    }
}