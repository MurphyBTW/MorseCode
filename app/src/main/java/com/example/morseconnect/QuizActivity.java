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
        txtMorseDisplay.setText(""); // reset
        long delay = 0;

        for (char c : code.toCharArray()) {
            if (c == '.' || c == '-') {
                char finalC = c;
                int duration = (c == '.' ? unitMs : unitMs * 3);

                handler.postDelayed(() -> {
                    txtMorseDisplay.append(String.valueOf(finalC)); // 🔥 SHOW DOT/DASH
                    startSignal();
                }, delay);

                handler.postDelayed(this::stopSignal, delay + duration);

                delay += duration + unitMs;
            } else if (c == ' ') {
                delay += unitMs * 2;
                handler.postDelayed(() -> txtMorseDisplay.append(" "), delay);
            }
        }
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

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
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