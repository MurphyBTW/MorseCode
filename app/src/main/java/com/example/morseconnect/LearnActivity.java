package com.example.morseconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Map;

public class LearnActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private Spinner spinnerLetters;

    private TextView txtLetter;
    private TextView txtMorse;
    private View flashIndicator;
    private Button btnPlay;

    private CameraManager cameraManager;
    private String cameraId;
    private AudioTrack audioTrack;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Runnable> playbackTasks = new ArrayList<>();

    private Map<String, Map<String, String>> categorizedMorse;
    private Map<String, String> selectedCategory;

    private int wpm = 20;
    private int frequency = 600;
    private float volume = 0.8f;
    private int unitMs = 60;
    private boolean flashEnabled = true;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerLetters = findViewById(R.id.spinnerLetters);

        txtLetter = findViewById(R.id.txtLetter);
        txtMorse = findViewById(R.id.txtMorse);
        flashIndicator = findViewById(R.id.flashIndicator);
        btnPlay = findViewById(R.id.btnPlay);

        cameraManager =
                (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();

            if (cameraIds.length > 0) {
                cameraId = cameraIds[0];
            }

        } catch (Exception ignored) {
            cameraId = null;
        }

        categorizedMorse = MorseDatabase.getCategorizedMorse();

        loadSettings();
        setupCategorySpinner();

        btnPlay.setOnClickListener(v -> {

            if (isPlaying) {
                cancelPlayback();
            } else {
                playSelectedMorse();
            }

        });
    }

    // =========================================================
    // SPINNER ADAPTER
    // =========================================================

    private ArrayAdapter<String> createWhiteSpinnerAdapter(
            ArrayList<String> items
    ) {

        return new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {

            private View styleSpinnerText(View view) {

                if (view instanceof TextView) {

                    TextView textView = (TextView) view;

                    textView.setTextColor(Color.WHITE);

                    // Keep spinner text smaller and clean.
                    textView.setTextSize(14);
                }

                return view;
            }

            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {

                View view = super.getView(
                        position,
                        convertView,
                        parent
                );

                return styleSpinnerText(view);
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {

                View view = super.getDropDownView(
                        position,
                        convertView,
                        parent
                );

                if (view instanceof TextView) {

                    TextView textView = (TextView) view;

                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(14);
                    textView.setBackgroundColor(
                            Color.rgb(23, 28, 39)
                    );

                    textView.setPadding(
                            24,
                            18,
                            24,
                            18
                    );
                }

                return view;
            }
        };
    }

    // =========================================================
    // CATEGORY SPINNER
    // =========================================================

    private void setupCategorySpinner() {

        if (categorizedMorse == null ||
                categorizedMorse.isEmpty()) {

            txtLetter.setText("No Morse data found");
            txtLetter.setTextSize(28);

            txtMorse.setText("");

            btnPlay.setEnabled(false);

            return;
        }

        ArrayList<String> categories =
                new ArrayList<>(
                        categorizedMorse.keySet()
                );

        ArrayAdapter<String> adapter =
                createWhiteSpinnerAdapter(categories);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        String category =
                                categories.get(position);

                        selectedCategory =
                                categorizedMorse.get(category);

                        setupLetterSpinner();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    // =========================================================
    // SIGNAL / LETTER SPINNER
    // =========================================================

    private void setupLetterSpinner() {

        cancelPlayback();

        if (selectedCategory == null ||
                selectedCategory.isEmpty()) {

            txtLetter.setText("No entries found");
            txtLetter.setTextSize(28);

            txtMorse.setText("");

            btnPlay.setEnabled(false);

            return;
        }

        btnPlay.setEnabled(true);

        ArrayList<String> entries =
                new ArrayList<>(
                        selectedCategory.keySet()
                );

        ArrayAdapter<String> adapter =
                createWhiteSpinnerAdapter(entries);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerLetters.setAdapter(adapter);

        spinnerLetters.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        cancelPlayback();

                        String entry =
                                entries.get(position);

                        txtLetter.setText(entry);

                        // Automatically resize the main signal
                        // depending on how long the text is.
                        adjustSignalTextSize(entry);

                        String code =
                                selectedCategory.get(entry);

                        txtMorse.setText(
                                code == null ? "" : code
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    // =========================================================
    // AUTOMATIC SIGNAL FONT SIZE
    // =========================================================

    private void adjustSignalTextSize(String entry) {

        if (entry == null ||
                entry.trim().isEmpty()) {

            txtLetter.setTextSize(64);
            return;
        }

        int length = entry.trim().length();

        /*
         * Examples:
         *
         * A / B / 1
         * 64sp
         *
         * SOS
         * 48sp
         *
         * HELLO
         * 48sp
         *
         * THANK YOU
         * 40sp
         *
         * AR (End of message)
         * 30sp
         */

        if (length <= 2) {

            // Letters / numbers
            txtLetter.setTextSize(64);

        } else if (length <= 6) {

            // Short words
            txtLetter.setTextSize(48);

        } else if (length <= 10) {

            // Medium words
            txtLetter.setTextSize(40);

        } else {

            // Prosigns and long descriptions
            txtLetter.setTextSize(30);
        }
    }

    // =========================================================
    // PLAY SELECTED MORSE
    // =========================================================

    private void playSelectedMorse() {

        if (selectedCategory == null ||
                spinnerLetters.getSelectedItem() == null) {

            return;
        }

        String entry =
                spinnerLetters
                        .getSelectedItem()
                        .toString();

        String code =
                selectedCategory.get(entry);

        if (code != null &&
                !code.trim().isEmpty()) {

            playMorse(code);
        }
    }

    // =========================================================
    // MORSE PLAYBACK
    // =========================================================

    private void playMorse(String code) {

        cancelPlayback();

        if (code == null ||
                code.trim().isEmpty()) {

            return;
        }

        isPlaying = true;
        btnPlay.setText("Stop");

        byte[] audioData =
                buildMorseAudio(code);

        if (audioData.length == 0) {

            cancelPlayback();
            return;
        }

        try {

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioData.length,
                    AudioTrack.MODE_STATIC
            );

            audioTrack.write(
                    audioData,
                    0,
                    audioData.length
            );

            audioTrack.play();

        } catch (Exception e) {

            releaseAudioTrack();
            cancelPlayback();

            return;
        }

        long delay = 0;

        for (char c : code.toCharArray()) {

            if (c == '.') {

                scheduleSignal(
                        delay,
                        unitMs
                );

                delay += unitMs * 2L;

            } else if (c == '-') {

                scheduleSignal(
                        delay,
                        unitMs * 3
                );

                delay += unitMs * 4L;

            } else if (c == ' ') {

                delay += unitMs * 2L;
            }
        }

        Runnable finishTask = () -> {

            setSignalVisual(false);

            releaseAudioTrack();

            isPlaying = false;

            btnPlay.setText("Play");

            playbackTasks.clear();
        };

        postPlayback(
                finishTask,
                delay + 80L
        );
    }

    // =========================================================
    // BUILD AUDIO
    // =========================================================

    private byte[] buildMorseAudio(String code) {

        final int sampleRate = 44100;

        int totalSamples = 0;

        for (char c : code.toCharArray()) {

            if (c == '.') {

                totalSamples +=
                        msToSamples(
                                unitMs * 2,
                                sampleRate
                        );

            } else if (c == '-') {

                totalSamples +=
                        msToSamples(
                                unitMs * 4,
                                sampleRate
                        );

            } else if (c == ' ') {

                totalSamples +=
                        msToSamples(
                                unitMs * 2,
                                sampleRate
                        );
            }
        }

        totalSamples +=
                msToSamples(
                        30,
                        sampleRate
                );

        short[] pcm =
                new short[
                        Math.max(
                                1,
                                totalSamples
                        )
                        ];

        int position = 0;

        for (char c : code.toCharArray()) {

            if (c == '.' || c == '-') {

                int toneMs =
                        c == '.'
                                ? unitMs
                                : unitMs * 3;

                int toneSamples =
                        msToSamples(
                                toneMs,
                                sampleRate
                        );

                writeSmoothTone(
                        pcm,
                        position,
                        toneSamples,
                        sampleRate
                );

                position +=
                        toneSamples +
                                msToSamples(
                                        unitMs,
                                        sampleRate
                                );

            } else if (c == ' ') {

                position +=
                        msToSamples(
                                unitMs * 2,
                                sampleRate
                        );
            }
        }

        byte[] data =
                new byte[pcm.length * 2];

        int index = 0;

        for (short value : pcm) {

            data[index++] =
                    (byte) (value & 0xFF);

            data[index++] =
                    (byte) ((value >> 8) & 0xFF);
        }

        return data;
    }

    private int msToSamples(
            int ms,
            int sampleRate
    ) {

        return Math.max(
                0,
                (int) Math.round(
                        sampleRate *
                                (ms / 1000.0)
                )
        );
    }

    private void writeSmoothTone(
            short[] buffer,
            int start,
            int length,
            int sampleRate
    ) {

        int safeFrequency =
                Math.max(
                        100,
                        frequency
                );

        int fadeSamples =
                Math.min(
                        msToSamples(
                                5,
                                sampleRate
                        ),
                        Math.max(
                                1,
                                length / 2
                        )
                );

        double phaseStep =
                2.0 *
                        Math.PI *
                        safeFrequency /
                        sampleRate;

        for (
                int i = 0;
                i < length &&
                        start + i < buffer.length;
                i++
        ) {

            double envelope = 1.0;

            if (i < fadeSamples) {

                envelope =
                        (double) i /
                                fadeSamples;

            } else if (
                    i >= length - fadeSamples
            ) {

                envelope =
                        (double)
                                (length - i - 1) /
                                fadeSamples;
            }

            envelope =
                    Math.max(
                            0.0,
                            Math.min(
                                    1.0,
                                    envelope
                            )
                    );

            buffer[start + i] =
                    (short) (
                            Math.sin(
                                    phaseStep * i
                            ) *
                                    32767.0 *
                                    volume *
                                    envelope
                    );
        }
    }

    // =========================================================
    // AUDIO CLEANUP
    // =========================================================

    private void releaseAudioTrack() {

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
    }

    // =========================================================
    // FLASH / VISUAL SIGNAL
    // =========================================================

    private void scheduleSignal(
            long startDelay,
            int duration
    ) {

        postPlayback(
                () -> setSignalVisual(true),
                startDelay
        );

        postPlayback(
                () -> setSignalVisual(false),
                startDelay + duration
        );
    }

    private void setSignalVisual(
            boolean active
    ) {

        setFlashlight(active);

        if (flashIndicator != null) {

            flashIndicator.setBackgroundResource(
                    active
                            ? R.drawable.indicator_on
                            : R.drawable.indicator_off
            );
        }
    }

    private void postPlayback(
            Runnable task,
            long delay
    ) {

        playbackTasks.add(task);

        handler.postDelayed(
                task,
                Math.max(
                        0,
                        delay
                )
        );
    }

    private void cancelPlayback() {

        for (Runnable task : playbackTasks) {

            handler.removeCallbacks(task);
        }

        playbackTasks.clear();

        stopSignal();

        isPlaying = false;

        if (btnPlay != null) {

            btnPlay.setText("Play");
        }
    }

    private void startSignal() {

        setSignalVisual(true);
    }

    private void stopSignal() {

        releaseAudioTrack();

        setSignalVisual(false);
    }

    private void setFlashlight(
            boolean enabled
    ) {

        if (cameraManager == null ||
                cameraId == null) {

            return;
        }

        try {

            cameraManager.setTorchMode(
                    cameraId,
                    enabled && flashEnabled
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SETTINGS
    // =========================================================

    private void loadSettings() {

        SharedPreferences prefs =
                getSharedPreferences(
                        "MorseSettings",
                        MODE_PRIVATE
                );

        frequency =
                prefs.getInt(
                        "frequency",
                        600
                );

        int volInt =
                prefs.getInt(
                        "volume",
                        80
                );

        volume =
                volInt / 100f;

        wpm =
                prefs.getInt(
                        "speed",
                        20
                );

        flashEnabled =
                prefs.getBoolean(
                        "flash_enabled",
                        true
                );

        unitMs =
                Math.max(
                        20,
                        1200 /
                                Math.max(
                                        5,
                                        wpm
                                )
                );
    }

    // =========================================================
    // ACTIVITY LIFECYCLE
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        loadSettings();
    }

    @Override
    protected void onPause() {

        cancelPlayback();

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        stopSignal();

        super.onDestroy();
    }
}