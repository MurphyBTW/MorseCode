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
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import java.util.*;

public class QuizActivity extends AppCompatActivity {

    TextView txtQuestion, txtMorseDisplay;
    MaterialButton btnChoice1, btnChoice2, btnChoice3, btnChoice4;
    Button btnPlay, btnSelect;
    View flashIndicator;

    String correctAnswer, selectedAnswer, currentMorse;
    String mode;

    Map<String, String> morseMap;
    List<String> keys;

    CameraManager cameraManager;
    String cameraId;

    Handler handler = new Handler();
    
    int wpm = 20;
    int frequency = 600;
    float volume = 0.8f;
    int unitMs = 60;

    AudioTrack audioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // 🔙 Back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                });

        txtQuestion = findViewById(R.id.txtQuestion);
        txtMorseDisplay = findViewById(R.id.txtMorseDisplay);
        btnChoice1 = findViewById(R.id.btnChoice1);
        btnChoice2 = findViewById(R.id.btnChoice2);
        btnChoice3 = findViewById(R.id.btnChoice3);
        btnChoice4 = findViewById(R.id.btnChoice4);
        btnPlay = findViewById(R.id.btnPlay);
        btnSelect = findViewById(R.id.btnSelect);
        flashIndicator = findViewById(R.id.flashIndicator);

        mode = getIntent().getStringExtra("mode");

        morseMap = getFullMorseMap();
        keys = new ArrayList<>(morseMap.keySet());

        loadSettings();

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception ignored) {}

        generateQuestion();

        btnChoice1.setOnClickListener(v -> selectAnswer(btnChoice1));
        btnChoice2.setOnClickListener(v -> selectAnswer(btnChoice2));
        btnChoice3.setOnClickListener(v -> selectAnswer(btnChoice3));
        btnChoice4.setOnClickListener(v -> selectAnswer(btnChoice4));

        btnPlay.setOnClickListener(v -> playMorse(currentMorse));

        btnSelect.setOnClickListener(v -> {
            if (selectedAnswer == null) return;

            if (selectedAnswer.equals(correctAnswer)) {
                Toast.makeText(this, "✅ Correct", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Wrong: " + correctAnswer, Toast.LENGTH_LONG).show();
            }

            generateQuestion();
        });
    }

    private void selectAnswer(MaterialButton btn) {
        // Reset styles for all choices
        List<MaterialButton> buttons = Arrays.asList(btnChoice1, btnChoice2, btnChoice3, btnChoice4);
        for (MaterialButton b : buttons) {
            b.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            b.setTextColor(getResources().getColor(R.color.link_color));
            b.setStrokeColorResource(R.color.link_color);
        }

        // Highlight selected button with GRAY
        btn.setBackgroundColor(getResources().getColor(R.color.gray));
        btn.setTextColor(getResources().getColor(android.R.color.white));
        btn.setStrokeColorResource(android.R.color.transparent);

        if (mode.equals("hard")) {
            selectedAnswer = btn.getText().toString();
        } else {
            selectedAnswer = (String) btn.getTag();
            playMorse(morseMap.get(selectedAnswer));
        }
    }

    private void generateQuestion() {
        Random r = new Random();
        List<String> pool = new ArrayList<>();

        // Reset button styles for new question
        List<MaterialButton> buttons = Arrays.asList(btnChoice1, btnChoice2, btnChoice3, btnChoice4);
        for (MaterialButton b : buttons) {
            b.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            b.setTextColor(getResources().getColor(R.color.link_color));
            b.setStrokeColorResource(R.color.link_color);
        }

        if (mode.equals("easy")) {
            for (String k : keys) {
                if (k.matches("[A-Z]")) pool.add(k);
            }
        } else if (mode.equals("medium")) {
            for (String k : keys) {
                if (k.matches("[0-9]") || isSymbol(k)) pool.add(k);
            }
        } else {
            pool = keys;
        }

        String key = pool.get(r.nextInt(pool.size()));
        String morse = morseMap.get(key);

        correctAnswer = key;
        currentMorse = morse;
        selectedAnswer = null;

        txtMorseDisplay.setText(""); // clear display

        List<String> choices = new ArrayList<>();
        choices.add(key);

        while (choices.size() < 4) {
            String wrong = pool.get(r.nextInt(pool.size()));
            if (!choices.contains(wrong)) {
                choices.add(wrong);
            }
        }
        Collections.shuffle(choices);

        MaterialButton[] btnArray = {btnChoice1, btnChoice2, btnChoice3, btnChoice4};

        if (!mode.equals("hard")) {
            txtQuestion.setText("Morse of \"" + key + "\"?");
            for (int i = 0; i < 4; i++) {
                btnArray[i].setTag(choices.get(i));
                btnArray[i].setText("Choice " + (char)('A' + i));
            }
            btnPlay.setVisibility(View.GONE);
        } else {
            txtQuestion.setText("What is this Morse?");
            for (int i = 0; i < 4; i++) {
                btnArray[i].setText(choices.get(i));
            }
            btnPlay.setVisibility(View.VISIBLE);
            playMorse(morse);
        }
    }

    private boolean isSymbol(String s) {
        return s.equals(".") || s.equals(",") || s.equals("?") || s.equals("!");
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
        handler.removeCallbacksAndMessages(null);
        stopSignal();
        txtMorseDisplay.setText("");

        if (code == null || code.trim().isEmpty()) return;

        byte[] audioData = buildMorseAudio(code);
        if (audioData.length == 0) return;

        try {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioData.length,
                    AudioTrack.MODE_STATIC
            );
            audioTrack.write(audioData, 0, audioData.length);
            audioTrack.play();
        } catch (Exception e) {
            releaseAudioTrack();
            return;
        }

        long delay = 0;
        for (char c : code.toCharArray()) {
            if (c == '.' || c == '-') {
                final char mark = c;
                int duration = c == '.' ? unitMs : unitMs * 3;
                handler.postDelayed(() -> {
                    txtMorseDisplay.append(String.valueOf(mark));
                    setSignalVisual(true);
                }, delay);
                handler.postDelayed(() -> setSignalVisual(false), delay + duration);
                delay += duration + unitMs;
            } else if (c == ' ') {
                final long spaceDelay = delay;
                handler.postDelayed(() -> txtMorseDisplay.append(" "), spaceDelay);
                delay += unitMs * 2L;
            }
        }

        handler.postDelayed(() -> {
            setSignalVisual(false);
            releaseAudioTrack();
        }, delay + 80L);
    }

    private byte[] buildMorseAudio(String code) {
        final int sampleRate = 44100;
        int totalSamples = 0;
        for (char c : code.toCharArray()) {
            if (c == '.') totalSamples += msToSamples(unitMs * 2, sampleRate);
            else if (c == '-') totalSamples += msToSamples(unitMs * 4, sampleRate);
            else if (c == ' ') totalSamples += msToSamples(unitMs * 2, sampleRate);
        }
        totalSamples += msToSamples(30, sampleRate);

        short[] pcm = new short[Math.max(1, totalSamples)];
        int position = 0;
        for (char c : code.toCharArray()) {
            if (c == '.' || c == '-') {
                int toneMs = c == '.' ? unitMs : unitMs * 3;
                int toneSamples = msToSamples(toneMs, sampleRate);
                writeSmoothTone(pcm, position, toneSamples, sampleRate);
                position += toneSamples + msToSamples(unitMs, sampleRate);
            } else if (c == ' ') {
                position += msToSamples(unitMs * 2, sampleRate);
            }
        }

        byte[] data = new byte[pcm.length * 2];
        int index = 0;
        for (short value : pcm) {
            data[index++] = (byte) (value & 0xFF);
            data[index++] = (byte) ((value >> 8) & 0xFF);
        }
        return data;
    }

    private int msToSamples(int ms, int sampleRate) {
        return Math.max(0, (int) Math.round(sampleRate * (ms / 1000.0)));
    }

    private void writeSmoothTone(short[] buffer, int start, int length, int sampleRate) {
        int safeFrequency = Math.max(100, frequency);
        int fadeSamples = Math.min(msToSamples(5, sampleRate), Math.max(1, length / 2));
        double phaseStep = 2.0 * Math.PI * safeFrequency / sampleRate;
        for (int i = 0; i < length && start + i < buffer.length; i++) {
            double envelope = 1.0;
            if (i < fadeSamples) envelope = (double) i / fadeSamples;
            else if (i >= length - fadeSamples) envelope = (double) (length - i - 1) / fadeSamples;
            envelope = Math.max(0.0, Math.min(1.0, envelope));
            buffer[start + i] = (short) (Math.sin(phaseStep * i) * 32767.0 * volume * envelope);
        }
    }

    private void setSignalVisual(boolean active) {
        if (cameraManager != null && cameraId != null) {
            try { cameraManager.setTorchMode(cameraId, active); } catch (Exception ignored) {}
        }
        flashIndicator.setBackgroundResource(active ? R.drawable.indicator_on : R.drawable.indicator_off);
    }

    private void releaseAudioTrack() {
        if (audioTrack != null) {
            try { audioTrack.stop(); } catch (Exception ignored) {}
            try { audioTrack.release(); } catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    private void startSignal() {
        setSignalVisual(true);
    }

    private void stopSignal() {
        releaseAudioTrack();
        setSignalVisual(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    private Map<String, String> getFullMorseMap() {
        Map<String, String> map = new HashMap<>();

        map.put("A", ".-"); map.put("B", "-...");
        map.put("C", "-.-."); map.put("D", "-..");
        map.put("E", "."); map.put("F", "..-.");
        map.put("G", "--."); map.put("H", "....");
        map.put("I", ".."); map.put("J", ".---");
        map.put("K", "-.-"); map.put("L", ".-..");
        map.put("M", "--"); map.put("N", "-.");
        map.put("O", "---"); map.put("P", ".--.");
        map.put("Q", "--.-"); map.put("R", ".-.");
        map.put("S", "..."); map.put("T", "-");

        map.put("1", ".----"); map.put("2", "..---");
        map.put("3", "...--"); map.put("4", "....-");
        map.put("5", ".....");

        map.put(".", ".-.-.-");
        map.put(",", "--..--");
        map.put("?", "..--..");
        map.put("!", "-.-.--");

        map.put("SOS", "... --- ...");
        map.put("HELLO", ".... . .-.. .-.. ---");

        map.put("AR", ".-.-.");
        map.put("SK", "...-.-");
        map.put("BT", "-...-");

        return map;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}