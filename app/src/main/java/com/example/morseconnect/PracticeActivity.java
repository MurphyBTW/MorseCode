package com.example.morseconnect;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import java.util.*;

public class PracticeActivity extends AppCompatActivity {

    TextView txtQuestion, txtInput, txtOutput;
    TextView txtSignalState, txtPressTime, txtInputCount, txtSpeedBadge, txtStreak;
    EditText edtInput;
    Button btnCheck, btnReset, btnDelete, btnMorse, btnPlayInput;
    View flashIndicator;
    MaterialCardView signalPanel;
    LinearLayout waveform;

    StringBuilder userInput = new StringBuilder();
    String correctAnswer;

    Map<String, String> morseMap;
    List<String> letters;

    long pressStartTime;
    int streak = 0;

    int wpm = 20;
    int frequency = 600;
    float volume = 0.8f;
    int unitMs = 60;

    AudioTrack audioTrack;
    final Handler handler = new Handler();
    CameraManager cameraManager;
    String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        txtQuestion = findViewById(R.id.txtQuestion);
        txtInput = findViewById(R.id.txtInput);
        txtOutput = findViewById(R.id.txtOutput);
        txtSignalState = findViewById(R.id.txtSignalState);
        txtPressTime = findViewById(R.id.txtPressTime);
        txtInputCount = findViewById(R.id.txtInputCount);
        txtSpeedBadge = findViewById(R.id.txtSpeedBadge);
        txtStreak = findViewById(R.id.txtStreak);

        edtInput = findViewById(R.id.edtInput);
        btnPlayInput = findViewById(R.id.btnPlayInput);
        btnCheck = findViewById(R.id.btnCheck);
        btnReset = findViewById(R.id.btnReset);
        btnDelete = findViewById(R.id.btnDelete);
        btnMorse = findViewById(R.id.btnMorse);
        flashIndicator = findViewById(R.id.flashIndicator);
        signalPanel = findViewById(R.id.signalPanel);
        waveform = findViewById(R.id.waveform);

        morseMap = MorseDatabase.getMorseMap();
        letters = new ArrayList<>(morseMap.keySet());
        Collections.sort(letters);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception ignored) {
            cameraId = null;
        }

        loadSettings();
        generateQuestion();
        updateSpeedBadge();
        updateInputUi();
        animateWaveformIdle();

        // Short press = dot, long press = dash.
        btnMorse.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pressStartTime = System.currentTimeMillis();
                    setSignalActive(true);
                    return true;

                case MotionEvent.ACTION_UP:
                    long duration = System.currentTimeMillis() - pressStartTime;
                    setSignalActive(false);

                    if (duration < Math.max(unitMs * 2L, 180L)) {
                        userInput.append('.');
                    } else {
                        userInput.append('-');
                    }

                    txtPressTime.setText(duration + " ms");
                    updateInputUi();
                    pulseTapButton();
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    setSignalActive(false);
                    return true;
            }
            return false;
        });

        btnCheck.setOnClickListener(v -> checkAnswer());

        btnDelete.setOnClickListener(v -> {
            userInput.setLength(0);
            txtPressTime.setText("0 ms");
            txtSignalState.setText("READY");
            txtSignalState.setTextColor(0xFF7F8BA3);
            updateInputUi();
        });

        btnReset.setOnClickListener(v -> {
            userInput.setLength(0);
            txtPressTime.setText("0 ms");
            generateQuestion();
            txtSignalState.setText("READY");
            txtSignalState.setTextColor(0xFF7F8BA3);
            updateInputUi();
            pulseQuestion();
        });

        btnPlayInput.setOnClickListener(v -> {
            String text = edtInput.getText().toString().toUpperCase(Locale.ROOT);
            String morse = convertToMorse(text);

            if (morse.isEmpty()) {
                txtOutput.setText("TYPE SOMETHING FIRST");
                return;
            }

            txtOutput.setText(morse);
            playMorse(morse);
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("MorseSettings", MODE_PRIVATE);
        frequency = prefs.getInt("frequency", 600);
        int volInt = prefs.getInt("volume", 80);
        volume = volInt / 100f;
        wpm = prefs.getInt("speed", 20);
        unitMs = Math.max(20, 1200 / Math.max(5, wpm));
    }

    private void updateSpeedBadge() {
        txtSpeedBadge.setText(wpm + " WPM");
    }

    private String convertToMorse(String text) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c == ' ') {
                result.append("   ");
            } else {
                String code = morseMap.get(String.valueOf(c));
                if (code != null) {
                    result.append(code).append(" ");
                }
            }
        }

        return result.toString().trim();
    }

    private void updateInputUi() {
        txtInput.setText(userInput.toString());
        int count = userInput.length();
        txtInputCount.setText(count + (count == 1 ? " mark" : " marks"));
    }

    private void setSignalActive(boolean active) {
        if (active) {
            startSignal();
            flashIndicator.setBackgroundResource(R.drawable.indicator_on_modern);
            txtSignalState.setText("TRANSMITTING");
            txtSignalState.setTextColor(0xFFD6A84F);
            signalPanel.setStrokeColor(0xFFD6A84F);
            animateWaveformActive();
        } else {
            stopSignal();
            flashIndicator.setBackgroundResource(R.drawable.indicator_off_modern);
            txtSignalState.setText("SIGNAL CAPTURED");
            txtSignalState.setTextColor(0xFF8C7CF2);
            signalPanel.setStrokeColor(0xFF263145);
            animateWaveformIdle();
        }
    }

    private void startSignal() {
        stopSignal();

        int sampleRate = 44100;
        double period = (double) sampleRate / Math.max(100, frequency);
        int numSamples = Math.max(441, (int) (Math.round(period) * 50));

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

        try {
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
        } catch (Exception ignored) {
            audioTrack = null;
        }

        if (cameraManager != null && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, true);
            } catch (Exception ignored) {
            }
        }
    }

    private void stopSignal() {
        if (audioTrack != null) {
            try {
                audioTrack.stop();
            } catch (Exception ignored) {
            }
            try {
                audioTrack.release();
            } catch (Exception ignored) {
            }
            audioTrack = null;
        }

        if (cameraManager != null && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (Exception ignored) {
            }
        }
    }

    private void playMorse(String code) {
        handler.removeCallbacksAndMessages(null);
        stopSignal();

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
            if (c == '.') {
                scheduleSignal(delay, unitMs);
                delay += unitMs * 2L;
            } else if (c == '-') {
                scheduleSignal(delay, unitMs * 3);
                delay += unitMs * 4L;
            } else if (c == ' ') {
                delay += unitMs * 2L;
            }
        }

        handler.postDelayed(() -> {
            setPlaybackVisual(false);
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
                position += toneSamples;
                position += msToSamples(unitMs, sampleRate);
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
            double sample = Math.sin(phaseStep * i) * envelope * volume;
            buffer[start + i] = (short) (sample * 32767.0);
        }
    }

    private void releaseAudioTrack() {
        if (audioTrack != null) {
            try { audioTrack.stop(); } catch (Exception ignored) {}
            try { audioTrack.release(); } catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    private void scheduleSignal(long start, int duration) {
        handler.postDelayed(() -> setPlaybackVisual(true), start);
        handler.postDelayed(() -> setPlaybackVisual(false), start + duration);
    }

    private void setPlaybackVisual(boolean active) {
        if (active) {
            flashIndicator.setBackgroundResource(R.drawable.indicator_on_modern);
            txtSignalState.setText("TRANSMITTING");
            txtSignalState.setTextColor(0xFFD6A84F);
            signalPanel.setStrokeColor(0xFFD6A84F);
            animateWaveformActive();
        } else {
            flashIndicator.setBackgroundResource(R.drawable.indicator_off_modern);
            txtSignalState.setText("SIGNAL CAPTURED");
            txtSignalState.setTextColor(0xFF8C7CF2);
            signalPanel.setStrokeColor(0xFF263145);
            animateWaveformIdle();
        }

        if (cameraManager != null && cameraId != null) {
            try { cameraManager.setTorchMode(cameraId, active); } catch (Exception ignored) {}
        }
    }

    private void generateQuestion() {
        if (letters.isEmpty()) return;

        Random random = new Random();
        String letter = letters.get(random.nextInt(letters.size()));
        correctAnswer = morseMap.get(letter);
        txtQuestion.setText(letter);
    }

    private void checkAnswer() {
        if (correctAnswer == null) return;

        if (userInput.toString().equals(correctAnswer)) {
            streak++;
            txtStreak.setText("STREAK " + streak);
            txtSignalState.setText("CORRECT  •  " + correctAnswer);
            txtSignalState.setTextColor(0xFF67D7A4);
            signalPanel.setStrokeColor(0xFF67D7A4);
            pulseQuestion();
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            streak = 0;
            txtStreak.setText("STREAK 0");
            txtSignalState.setText("TRY AGAIN  •  TARGET " + correctAnswer);
            txtSignalState.setTextColor(0xFFE87575);
            signalPanel.setStrokeColor(0xFFE87575);
            shakeSignalPanel();
            Toast.makeText(this, "Not quite. Try the signal again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void pulseTapButton() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator sx = ObjectAnimator.ofFloat(btnMorse, View.SCALE_X, 0.94f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(btnMorse, View.SCALE_Y, 0.94f, 1f);
        set.playTogether(sx, sy);
        set.setDuration(150);
        set.setInterpolator(new OvershootInterpolator());
        set.start();
    }

    private void pulseQuestion() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator sx = ObjectAnimator.ofFloat(txtQuestion, View.SCALE_X, 0.86f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(txtQuestion, View.SCALE_Y, 0.86f, 1f);
        set.playTogether(sx, sy);
        set.setDuration(260);
        set.setInterpolator(new OvershootInterpolator());
        set.start();
    }

    private void shakeSignalPanel() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(
                signalPanel,
                View.TRANSLATION_X,
                0, -12, 12, -8, 8, -3, 3, 0
        );
        shake.setDuration(360);
        shake.start();
    }

    private void animateWaveformActive() {
        for (int i = 0; i < waveform.getChildCount(); i++) {
            View bar = waveform.getChildAt(i);
            float target = 0.55f + ((i % 3) * 0.2f);
            ObjectAnimator animator = ObjectAnimator.ofFloat(bar, View.SCALE_Y, target, 1.15f, target);
            animator.setDuration(420L + (i * 35L));
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.setRepeatMode(ObjectAnimator.REVERSE);
            animator.start();
            bar.setTag(animator);
        }
    }

    private void animateWaveformIdle() {
        for (int i = 0; i < waveform.getChildCount(); i++) {
            View bar = waveform.getChildAt(i);
            Object tag = bar.getTag();
            if (tag instanceof ObjectAnimator) {
                ((ObjectAnimator) tag).cancel();
            }
            bar.setScaleY(1f);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        updateSpeedBadge();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        animateWaveformIdle();
        stopSignal();
        super.onDestroy();
    }
}
