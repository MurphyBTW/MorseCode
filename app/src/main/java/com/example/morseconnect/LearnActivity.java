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

        cameraManager = (CameraManager)
                getSystemService(Context.CAMERA_SERVICE);

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

    // Creates a Spinner adapter with white text for both
    // the selected item and the dropdown list.
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
                    textView.setBackgroundColor(Color.rgb(23, 28, 39));
                    textView.setPadding(24, 18, 24, 18);
                }

                return view;
            }
        };
    }

    private void setupCategorySpinner() {
        if (categorizedMorse == null || categorizedMorse.isEmpty()) {
            txtLetter.setText("No Morse data found");
            txtMorse.setText("");
            btnPlay.setEnabled(false);
            return;
        }

        ArrayList<String> categories =
                new ArrayList<>(categorizedMorse.keySet());

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
                        String category = categories.get(position);
                        selectedCategory = categorizedMorse.get(category);
                        setupLetterSpinner();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void setupLetterSpinner() {
        cancelPlayback();

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            txtLetter.setText("No entries found");
            txtMorse.setText("");
            btnPlay.setEnabled(false);
            return;
        }

        btnPlay.setEnabled(true);

        ArrayList<String> entries =
                new ArrayList<>(selectedCategory.keySet());

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

                        String entry = entries.get(position);
                        txtLetter.setText(entry);

                        String code = selectedCategory.get(entry);
                        txtMorse.setText(code == null ? "" : code);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void playSelectedMorse() {
        if (selectedCategory == null
                || spinnerLetters.getSelectedItem() == null) {
            return;
        }

        String entry = spinnerLetters.getSelectedItem().toString();
        String code = selectedCategory.get(entry);

        if (code != null && !code.trim().isEmpty()) {
            playMorse(code);
        }
    }

    private void playMorse(String code) {
        cancelPlayback();

        if (code == null || code.trim().isEmpty()) {
            return;
        }

        isPlaying = true;
        btnPlay.setText("Stop");

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

        Runnable finishTask = () -> {
            stopSignal();
            isPlaying = false;
            btnPlay.setText("Play");
            playbackTasks.clear();
        };

        postPlayback(finishTask, delay);
    }

    private void scheduleSignal(long startDelay, int duration) {
        Runnable startTask = this::startSignal;
        Runnable stopTask = this::stopSignal;

        postPlayback(startTask, startDelay);
        postPlayback(stopTask, startDelay + duration);
    }

    private void postPlayback(Runnable task, long delay) {
        playbackTasks.add(task);
        handler.postDelayed(task, Math.max(0, delay));
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

    // This uses the same tone generation as PracticeActivity.
    private void startSignal() {
        stopSignal();

        int sampleRate = 44100;
        double period = (double) sampleRate / Math.max(100, frequency);
        int numSamples = Math.max(
                441,
                (int) (Math.round(period) * 50)
        );

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

            audioTrack.write(
                    generatedSnd,
                    0,
                    generatedSnd.length
            );

            audioTrack.setLoopPoints(0, numSamples, -1);
            audioTrack.play();

        } catch (Exception ignored) {
            if (audioTrack != null) {
                try {
                    audioTrack.release();
                } catch (Exception ignoredAgain) {
                }
                audioTrack = null;
            }
        }

        setFlashlight(true);

        if (flashIndicator != null) {
            flashIndicator.setBackgroundResource(R.drawable.indicator_on);
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

        setFlashlight(false);

        if (flashIndicator != null) {
            flashIndicator.setBackgroundResource(R.drawable.indicator_off);
        }
    }

    private void setFlashlight(boolean enabled) {
        if (cameraManager == null || cameraId == null) {
            return;
        }

        try {
            cameraManager.setTorchMode(cameraId, enabled);
        } catch (Exception ignored) {
        }
    }

    private void loadSettings() {
        SharedPreferences prefs =
                getSharedPreferences("MorseSettings", MODE_PRIVATE);

        frequency = prefs.getInt("frequency", 600);
        int volInt = prefs.getInt("volume", 80);
        volume = volInt / 100f;
        wpm = prefs.getInt("speed", 20);

        unitMs = Math.max(20, 1200 / Math.max(5, wpm));
    }

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