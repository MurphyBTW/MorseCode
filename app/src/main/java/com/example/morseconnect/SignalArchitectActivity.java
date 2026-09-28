
package com.example.morseconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class SignalArchitectActivity extends AppCompatActivity {

    private static final int TARGET = 20;
    private static final String PREFS = "signal_architect_stats";

    private TextView txtRound, txtTime, txtCorrect, txtSignal;
    private TextView txtFeedback, txtCombo, txtProfileStats;
    private Button[] answerButtons;
    private NetworkMapView networkMap;

    private final HashMap<String, String> morse = new HashMap<>();
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int correct = 0;
    private int combo = 0;
    private int wins = 0;
    private int losses = 0;

    private long bestTime = 0;
    private long startTime = 0;

    private boolean gameOver = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!gameOver) {
                updateTimer();
                handler.postDelayed(this, 250);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal_architect);

        bindViews();
        setupMorse();
        loadStats();
        updateProfileStats();
        setupAnswerButtons();

        startGame();
    }

    private void bindViews() {
        txtRound = findViewById(R.id.txtRound);
        txtTime = findViewById(R.id.txtTime);
        txtCorrect = findViewById(R.id.txtCorrect);
        txtSignal = findViewById(R.id.txtSignal);
        txtFeedback = findViewById(R.id.txtFeedback);
        txtCombo = findViewById(R.id.txtCombo);
        txtProfileStats = findViewById(R.id.txtProfileStats);

        networkMap = findViewById(R.id.networkMap);

        answerButtons = new Button[]{
                findViewById(R.id.btnAnswer1),
                findViewById(R.id.btnAnswer2),
                findViewById(R.id.btnAnswer3),
                findViewById(R.id.btnAnswer4)
        };

        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    private void setupMorse() {
        String[] letters = {
                "A", "B", "C", "D", "E", "F", "G",
                "H", "I", "J", "K", "L", "M", "N",
                "O", "P", "Q", "R", "S", "T", "U",
                "V", "W", "X", "Y", "Z"
        };

        String[] codes = {
                "· —",       // A
                "— · · ·",   // B
                "— · — ·",   // C
                "— · ·",     // D
                "·",         // E
                "· · — ·",   // F
                "— — ·",     // G
                "· · · ·",   // H
                "· ·",       // I
                "· — — —",   // J
                "— · —",     // K
                "· — · ·",   // L
                "— —",       // M
                "— ·",       // N
                "— — —",     // O
                "· — — ·",   // P
                "— — · —",   // Q
                "· — ·",     // R
                "· · ·",     // S
                "—",         // T
                "· · —",     // U
                "· · · —",   // V
                "· — —",     // W
                "— · · —",   // X
                "— · — —",   // Y
                "— — · ·"    // Z
        };

        for (int i = 0; i < letters.length; i++) {
            morse.put(letters[i], codes[i]);
        }

        String[] digits = {
                "— — — — —", // 0
                "· — — — —", // 1
                "· · — — —", // 2
                "· · · — —", // 3
                "· · · · —", // 4
                "· · · · ·", // 5
                "— · · · ·", // 6
                "— — · · ·", // 7
                "— — — · ·", // 8
                "— — — — ·"  // 9
        };

        for (int i = 0; i < digits.length; i++) {
            morse.put(String.valueOf(i), digits[i]);
        }
    }

    private void setupAnswerButtons() {
        for (Button button : answerButtons) {
            button.setOnClickListener(v -> {
                if (gameOver) return;

                checkAnswer(button.getText().toString());
            });
        }
    }

    private void startGame() {
        correct = 0;
        combo = 0;
        gameOver = false;
        startTime = SystemClock.elapsedRealtime();

        txtFeedback.setText("One mistake ends your run.");
        txtFeedback.setTextColor(Color.rgb(170, 181, 211));

        setButtonsEnabled(true);
        updateProgress();
        loadQuestion();

        handler.removeCallbacks(timerRunnable);
        handler.post(timerRunnable);
    }

    private ArrayList<String> getLetterPool(int round) {
        ArrayList<String> pool = new ArrayList<>();

        String[] easy = {
                "E", "T", "I", "M", "A", "N"
        };

        String[] medium = {
                "E", "T", "I", "M", "A", "N",
                "S", "O", "R", "H", "D", "L", "U"
        };

        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        if (round <= 5) {
            Collections.addAll(pool, easy);

        } else if (round <= 10) {
            Collections.addAll(pool, medium);

        } else if (round <= 15) {
            for (char c : alphabet.toCharArray()) {
                pool.add(String.valueOf(c));
            }

        } else {
            for (char c : alphabet.toCharArray()) {
                pool.add(String.valueOf(c));
            }

            for (int i = 0; i <= 9; i++) {
                pool.add(String.valueOf(i));
            }
        }

        return pool;
    }

    private void loadQuestion() {
        if (gameOver) return;

        int round = correct + 1;
        ArrayList<String> pool = getLetterPool(round);

        String answer = pool.get(random.nextInt(pool.size()));

        ArrayList<String> options = new ArrayList<>();
        options.add(answer);

        while (options.size() < 4) {
            String choice = pool.get(random.nextInt(pool.size()));

            if (!options.contains(choice)) {
                options.add(choice);
            }
        }

        Collections.shuffle(options);

        txtSignal.setText(morse.get(answer));
        txtSignal.setTag(answer);
        txtRound.setText(round + " / " + TARGET);

        for (int i = 0; i < answerButtons.length; i++) {
            Button button = answerButtons[i];

            button.setText(options.get(i));
            button.setEnabled(true);
            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.rgb(52, 48, 120)
                    )
            );
        }
    }

    private void checkAnswer(String selected) {
        if (gameOver) return;

        Object tag = txtSignal.getTag();
        if (tag == null) return;

        String answer = tag.toString();

        if (selected.equals(answer)) {
            correct++;
            combo++;

            txtFeedback.setText(
                    "Signal accepted. Connection restored."
            );
            txtFeedback.setTextColor(
                    Color.rgb(85, 233, 242)
            );

            updateProgress();
            showMilestone();

            if (correct >= TARGET) {
                winGame();
            } else {
                setButtonsEnabled(false);

                handler.postDelayed(() -> {
                    if (!gameOver) {
                        loadQuestion();
                    }
                }, 250);
            }

        } else {
            // No answer reveal. One mistake ends the run.
            loseGame();
        }
    }

    private void updateProgress() {
        txtCorrect.setText(String.valueOf(correct));
        txtCombo.setText("🔥 " + combo);

        int round = Math.min(correct + 1, TARGET);
        txtRound.setText(round + " / " + TARGET);

        networkMap.setCorrect(correct);
    }

    private void showMilestone() {
        if (combo == 5 || combo == 10 || combo == 15) {
            Toast.makeText(
                    this,
                    "🔥 " + combo + " STREAK!",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        for (Button button : answerButtons) {
            button.setEnabled(enabled);
        }
    }

    private void updateTimer() {
        long elapsed =
                SystemClock.elapsedRealtime() - startTime;

        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        txtTime.setText(String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        ));
    }

    private long getElapsedTime() {
        return (SystemClock.elapsedRealtime() - startTime)
                / 1000;
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                remainingSeconds
        );
    }

    private void winGame() {
        if (gameOver) return;

        gameOver = true;
        handler.removeCallbacks(timerRunnable);
        setButtonsEnabled(false);

        long elapsed = getElapsedTime();

        wins++;

        if (bestTime == 0 || elapsed < bestTime) {
            bestTime = elapsed;
        }

        saveStats();
        updateProfileStats();

        showResultDialog(
                "NETWORK RESTORED",
                "You completed all 20 rounds!\n\n"
                        + "Correct signals: 20 / 20\n"
                        + "Time: " + formatTime(elapsed)
                        + "\nBest time: " + formatTime(bestTime)
                        + "\n\nVictory!"
        );
    }

    private void loseGame() {
        if (gameOver) return;

        gameOver = true;
        handler.removeCallbacks(timerRunnable);
        setButtonsEnabled(false);

        losses++;

        saveStats();
        updateProfileStats();

        long elapsed = getElapsedTime();

        showResultDialog(
                "SIGNAL LOST",
                "Your run has ended.\n\n"
                        + "Correct signals: " + correct + " / 20\n"
                        + "Time survived: " + formatTime(elapsed)
                        + "\n\nNo second chances. Try again."
        );
    }

    private void showResultDialog(
            String title,
            String message
    ) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        "Play Again",
                        (dialog, which) -> startGame()
                )
                .setNegativeButton(
                        "Back",
                        (dialog, which) -> finish()
                )
                .show();
    }

    private void loadStats() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        wins = prefs.getInt("wins", 0);
        losses = prefs.getInt("losses", 0);
        bestTime = prefs.getLong("best_time", 0);
    }

    private void saveStats() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt("wins", wins)
                .putInt("losses", losses)
                .putLong("best_time", bestTime)
                .apply();
    }

    private void updateProfileStats() {
        int total = wins + losses;

        int winRate = total == 0
                ? 0
                : Math.round((wins * 100f) / total);

        String best = bestTime == 0
                ? "--:--"
                : formatTime(bestTime);

        txtProfileStats.setText(
                "WINS " + wins
                        + "     LOSSES " + losses
                        + "     WIN RATE " + winRate + "%"
                        + "     BEST " + best
        );
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }
}