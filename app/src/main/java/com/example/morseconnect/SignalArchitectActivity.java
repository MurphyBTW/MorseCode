package com.example.morseconnect;

import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SignalArchitectActivity extends AppCompatActivity {

    private static final int TARGET = 20;

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private TextView txtRound;
    private TextView txtTime;
    private TextView txtCorrect;
    private TextView txtSignal;
    private TextView txtMmr;
    private TextView txtFeedback;
    private TextView txtCombo;
    private TextView txtProfileStats;

    private Button[] answerButtons;

    private NetworkMapView networkMap;

    private AlertDialog countdownDialog;
    private Runnable countdownRunnable;

    private SharedPreferences sessionPrefs;

    private RequestQueue requestQueue;

    private CameraManager cameraManager;
    private String cameraId;
    private boolean flashEnabled = true;
    private int flashUnitMs = 60;

    private final Handler signalHandler =
            new Handler(Looper.getMainLooper());

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Random random =
            new Random();

    private int correct = 0;
    private int combo = 0;
    private int elapsedSeconds = 0;

    private boolean gameOver = false;
    private boolean statsLoaded = false;
    private boolean submittingResult = false;

    private String currentAnswer = "";
    private String previousAnswer = "";

    // ------------------------------------------------
    // SERVER STATS
    // ------------------------------------------------

    private int serverMmr = 1000;
    private int serverWins = 0;
    private int serverLosses = 0;
    private int serverCurrentStreak = 0;
    private int serverBestStreak = 0;

    private final ArrayList<String> questionDeck =
            new ArrayList<>();

    private int deckPosition = 0;

    private final String[] letters = {
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z"
    };

    private final String[] numbers = {
            "0", "1", "2", "3", "4",
            "5", "6", "7", "8", "9"
    };

    private final String[] punctuation = {
            ".", ",", "?", "'", "!", "/",
            "(", ")", "&", ":", ";",
            "=", "+", "-", "_", "\"",
            "$", "@"
    };

    private final String[] morse = {

            // A-Z
            ".-", "-...", "-.-.", "-..", ".",
            "..-.", "--.", "....", "..", ".---",
            "-.-", ".-..", "--", "-.", "---",
            ".--.", "--.-", ".-.", "...", "-",
            "..-", "...-", ".--", "-..-", "-.--",
            "--..",

            // 0-9
            "-----", ".----", "..---", "...--", "....-",
            ".....", "-....", "--...", "---..", "----.",

            // punctuation
            ".-.-.-", "--..--", "..--..", ".----.",
            "-.-.--", "-..-.", "-.--.", "-.--.-",
            ".-...", "---...", "-.-.-.", "-...-",
            ".-.-.", "-....-", "..--.-", ".-..-.",
            "...-..-", ".--.-."
    };

    private final ArrayList<String> allCharacters =
            new ArrayList<>();

    // ------------------------------------------------
    // TIMER
    // ------------------------------------------------

    private final Runnable timerRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (gameOver) {
                        return;
                    }

                    elapsedSeconds++;

                    updateTimer();

                    handler.postDelayed(
                            this,
                            1000
                    );
                }
            };

    // ------------------------------------------------
    // ON CREATE
    // ------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_signal_architect
        );

        sessionPrefs =
                getSharedPreferences(
                        SESSION_PREFS,
                        MODE_PRIVATE
                );

        requestQueue =
                Volley.newRequestQueue(this);

        bindViews();

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            cameraId = ids.length > 0 ? ids[0] : null;
        } catch (Exception ignored) {
            cameraId = null;
        }

        loadSignalSettings();

        setupCharacters();

        // Show cached server stats while refreshing.
        loadCachedStats();

        // Get authoritative stats from Supabase.
        loadProfileStats();
    }

    // ------------------------------------------------
    // BIND VIEWS
    // ------------------------------------------------

    private void bindViews() {

        txtRound =
                findViewById(R.id.txtRound);

        txtTime =
                findViewById(R.id.txtTime);

        txtMmr =
                findViewById(R.id.txtMmr);

        txtCorrect =
                findViewById(R.id.txtCorrect);

        txtSignal =
                findViewById(R.id.txtSignal);

        txtFeedback =
                findViewById(R.id.txtFeedback);

        txtCombo =
                findViewById(R.id.txtCombo);

        txtProfileStats =
                findViewById(R.id.txtProfileStats);

        networkMap =
                findViewById(R.id.networkMap);

        answerButtons =
                new Button[]{
                        findViewById(R.id.btnAnswer1),
                        findViewById(R.id.btnAnswer2),
                        findViewById(R.id.btnAnswer3),
                        findViewById(R.id.btnAnswer4)
                };

        findViewById(R.id.btnExit)
                .setOnClickListener(
                        v -> finish()
                );
    }

    // ------------------------------------------------
    // LOAD CACHED STATS
    // ------------------------------------------------

    private void loadCachedStats() {

        serverMmr =
                sessionPrefs.getInt(
                        "mmr",
                        1000
                );

        serverWins =
                sessionPrefs.getInt(
                        "wins",
                        0
                );

        serverLosses =
                sessionPrefs.getInt(
                        "losses",
                        0
                );

        serverCurrentStreak =
                sessionPrefs.getInt(
                        "current_streak",
                        0
                );

        serverBestStreak =
                sessionPrefs.getInt(
                        "best_streak",
                        0
                );

        updateStats();
    }

    // ------------------------------------------------
    // GET CURRENT STATS FROM PROFILE ENDPOINT
    // ------------------------------------------------

    private void loadProfileStats() {

        String token =
                getSessionToken();

        if (token.isEmpty()) {

            showSessionError();

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "profile";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            try {

                                boolean success =
                                        response.optBoolean(
                                                "success",
                                                false
                                        );

                                if (!success) {

                                    Toast.makeText(
                                            this,
                                            response.optString(
                                                    "message",
                                                    "Unable to load stats"
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                JSONObject stats =
                                        response.optJSONObject(
                                                "stats"
                                        );

                                if (stats == null) {

                                    Toast.makeText(
                                            this,
                                            "Player stats were not returned",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                updateServerStats(
                                        stats
                                );

                                statsLoaded = true;

                                showReadyDialog();

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "Unable to read player stats",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        },

                        error -> {

                            if (
                                    error.networkResponse != null &&
                                            error.networkResponse.statusCode == 401
                            ) {

                                showSessionError();

                                return;
                            }

                            Toast.makeText(
                                    this,
                                    "Unable to load competitive stats",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                ) {

                    @Override
                    public Map<String, String> getHeaders() {

                        return buildHeaders();
                    }
                };

        request.setRetryPolicy(
                new DefaultRetryPolicy(
                        15000,
                        0,
                        1.0f
                )
        );

        requestQueue.add(request);
    }

    // ------------------------------------------------
    // SETUP CHARACTERS
    // ------------------------------------------------

    private void setupCharacters() {

        allCharacters.clear();

        Collections.addAll(
                allCharacters,
                letters
        );

        Collections.addAll(
                allCharacters,
                numbers
        );

        Collections.addAll(
                allCharacters,
                punctuation
        );
    }

    // ------------------------------------------------
    // START GAME
    // ------------------------------------------------

    private void startGame() {

        if (!statsLoaded) {

            Toast.makeText(
                    this,
                    "Waiting for your competitive profile",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        handler.removeCallbacks(
                timerRunnable
        );

        signalHandler.removeCallbacksAndMessages(null);
        setTorch(false);

        correct = 0;
        combo = 0;
        elapsedSeconds = 0;
        gameOver = false;
        submittingResult = false;

        previousAnswer = "";
        currentAnswer = "";

        deckPosition = 0;

        txtFeedback.setText("");

        txtCombo.setText(
                "COMBO x0"
        );

        updateStats();

        updateTimer();

        if (networkMap != null) {

            networkMap.setCorrect(0);
        }

        handler.postDelayed(
                timerRunnable,
                1000
        );

        loadQuestion();
    }

    // ------------------------------------------------
    // LOAD QUESTION
    // ------------------------------------------------

    private void loadQuestion() {

        if (gameOver) {
            return;
        }

        int round =
                correct + 1;

        if (correct >= TARGET) {

            winGame();

            return;
        }

        ArrayList<String> pool =
                getPoolForRound(round);

        if (
                questionDeck.isEmpty() ||
                        deckPosition >= questionDeck.size() ||
                        !questionDeck.containsAll(pool)
        ) {

            questionDeck.clear();

            questionDeck.addAll(pool);

            Collections.shuffle(
                    questionDeck
            );

            deckPosition = 0;
        }

        String next =
                questionDeck.get(
                        deckPosition
                );

        if (
                next.equals(previousAnswer) &&
                        questionDeck.size() > 1
        ) {

            Collections.swap(
                    questionDeck,
                    deckPosition,
                    (deckPosition + 1) %
                            questionDeck.size()
            );

            next =
                    questionDeck.get(
                            deckPosition
                    );
        }

        currentAnswer = next;

        previousAnswer =
                currentAnswer;

        deckPosition++;

        txtRound.setText(
                "ROUND " +
                        round +
                        " / " +
                        TARGET
        );

        txtCorrect.setText(
                "CORRECT: " +
                        correct
        );

        txtFeedback.setText("");

        String currentMorse =
                getMorse(
                        currentAnswer
                );

        txtSignal.setText(
                currentMorse
        );

        playFlashMorse(
                currentMorse
        );

        ArrayList<String> choices =
                new ArrayList<>();

        choices.add(
                currentAnswer
        );

        Set<String> used =
                new HashSet<>();

        used.add(
                currentAnswer
        );

        while (
                choices.size() < 4
        ) {

            String option =
                    pool.get(
                            random.nextInt(
                                    pool.size()
                            )
                    );

            if (
                    used.add(option)
            ) {

                choices.add(option);
            }
        }

        Collections.shuffle(
                choices
        );

        for (
                int i = 0;
                i < answerButtons.length;
                i++
        ) {

            Button button =
                    answerButtons[i];

            button.setText(
                    choices.get(i)
            );

            button.setEnabled(true);

            button.setOnClickListener(
                    v ->
                            checkAnswer(
                                    button
                                            .getText()
                                            .toString()
                            )
            );
        }
    }

    // ------------------------------------------------
    // QUESTION POOL
    // ------------------------------------------------

    private ArrayList<String> getPoolForRound(
            int round
    ) {

        ArrayList<String> pool =
                new ArrayList<>();

        if (round <= 5) {

            Collections.addAll(
                    pool,
                    "E", "T", "A",
                    "I", "N", "M"
            );

        } else if (round <= 10) {

            Collections.addAll(
                    pool,
                    letters
            );

        } else if (round <= 15) {

            Collections.addAll(
                    pool,
                    letters
            );

            Collections.addAll(
                    pool,
                    numbers
            );

        } else {

            Collections.addAll(
                    pool,
                    letters
            );

            Collections.addAll(
                    pool,
                    numbers
            );

            Collections.addAll(
                    pool,
                    punctuation
            );
        }

        return pool;
    }

    // ------------------------------------------------
    // MORSE
    // ------------------------------------------------

    private String getMorse(
            String character
    ) {

        int index = -1;

        for (
                int i = 0;
                i < letters.length;
                i++
        ) {

            if (
                    letters[i]
                            .equals(character)
            ) {

                index = i;

                break;
            }
        }

        if (index >= 0) {

            return morse[index];
        }

        for (
                int i = 0;
                i < numbers.length;
                i++
        ) {

            if (
                    numbers[i]
                            .equals(character)
            ) {

                return morse[
                        letters.length + i
                        ];
            }
        }

        for (
                int i = 0;
                i < punctuation.length;
                i++
        ) {

            if (
                    punctuation[i]
                            .equals(character)
            ) {

                return morse[
                        letters.length +
                                numbers.length +
                                i
                        ];
            }
        }

        return "";
    }

    // ------------------------------------------------
    // CHECK ANSWER
    // ------------------------------------------------

    private void checkAnswer(
            String selected
    ) {

        if (
                gameOver ||
                        submittingResult
        ) {

            return;
        }

        if (
                selected.equals(
                        currentAnswer
                )
        ) {

            correct++;

            combo++;

            txtFeedback.setText(
                    "CORRECT!"
            );

            txtCorrect.setText(
                    "CORRECT: " +
                            correct
            );

            txtCombo.setText(
                    "COMBO x" +
                            combo
            );

            if (
                    networkMap != null
            ) {

                networkMap.setCorrect(
                        correct
                );
            }

            if (
                    combo == 5 ||
                            combo == 10 ||
                            combo == 15
            ) {

                Toast.makeText(
                        this,
                        combo + " STREAK!",
                        Toast.LENGTH_SHORT
                ).show();
            }

            disableAnswerButtons();

            if (
                    correct >= TARGET
            ) {

                handler.postDelayed(
                        this::winGame,
                        300
                );

            } else {

                handler.postDelayed(
                        this::loadQuestion,
                        300
                );
            }

        } else {

            combo = 0;

            loseGame();
        }
    }

    // ------------------------------------------------
    // WIN
    // ------------------------------------------------

    private void winGame() {

        if (
                gameOver ||
                        submittingResult
        ) {

            return;
        }

        gameOver = true;

        submittingResult = true;

        handler.removeCallbacks(
                timerRunnable
        );

        signalHandler.removeCallbacksAndMessages(null);
        setTorch(false);

        disableAnswerButtons();

        submitMatchResult(
                "win",
                elapsedSeconds,
                0
        );
    }

    // ------------------------------------------------
    // LOSS
    // ------------------------------------------------

    private void loseGame() {

        if (
                gameOver ||
                        submittingResult
        ) {

            return;
        }

        gameOver = true;

        submittingResult = true;

        handler.removeCallbacks(
                timerRunnable
        );

        signalHandler.removeCallbacksAndMessages(null);
        setTorch(false);

        disableAnswerButtons();

        int questionNumber =
                Math.min(
                        TARGET,
                        correct + 1
                );

        submitMatchResult(
                "loss",
                0,
                questionNumber
        );
    }

    // ------------------------------------------------
    // SUBMIT RESULT TO SUPABASE
    // ------------------------------------------------

    private void submitMatchResult(
            String result,
            int elapsed,
            int questionNumber
    ) {

        String token =
                getSessionToken();

        if (token.isEmpty()) {

            submittingResult = false;

            showSessionError();

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "record-match";

        JSONObject body =
                new JSONObject();

        try {

            body.put(
                    "result",
                    result
            );

            if (
                    result.equals("win")
            ) {

                body.put(
                        "elapsed_seconds",
                        elapsed
                );

            } else {

                body.put(
                        "question_number",
                        questionNumber
                );
            }

        } catch (JSONException e) {

            submittingResult = false;

            Toast.makeText(
                    this,
                    "Unable to prepare match result",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            submittingResult = false;

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                Toast.makeText(
                                        this,
                                        response.optString(
                                                "message",
                                                "Unable to save match"
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            JSONObject stats =
                                    response.optJSONObject(
                                            "stats"
                                    );

                            if (stats == null) {

                                Toast.makeText(
                                        this,
                                        "Match saved, but stats were not returned",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            updateServerStats(
                                    stats
                            );

                            int mmrChange =
                                    response.optInt(
                                            "mmr_change",
                                            0
                                    );

                            if (
                                    result.equals("win")
                            ) {

                                showWinDialog(
                                        mmrChange
                                );

                            } else {

                                showLossDialog(
                                        mmrChange
                                );
                            }
                        },

                        error -> {

                            submittingResult = false;

                            if (
                                    error.networkResponse != null &&
                                            error.networkResponse.statusCode == 401
                            ) {

                                showSessionError();

                                return;
                            }

                            /*
                             * IMPORTANT:
                             * We do NOT update MMR locally here.
                             *
                             * If the server request fails,
                             * the database remains the authority.
                             */

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            "RESULT NOT SAVED"
                                    )
                                    .setMessage(
                                            "MorseConnect couldn't save this match result. Check your connection and try again."
                                    )
                                    .setCancelable(false)
                                    .setPositiveButton(
                                            "BACK",
                                            (dialog, which) ->
                                                    finish()
                                    )
                                    .show();
                        }
                ) {

                    @Override
                    public Map<String, String> getHeaders() {

                        return buildHeaders();
                    }
                };

        /*
         * No automatic retry.
         *
         * This is important for a match-result endpoint.
         * An automatic retry could potentially submit
         * the same result twice.
         */

        request.setRetryPolicy(
                new DefaultRetryPolicy(
                        15000,
                        0,
                        1.0f
                )
        );

        requestQueue.add(
                request
        );
    }

    // ------------------------------------------------
    // UPDATE SERVER STATS
    // ------------------------------------------------

    private void updateServerStats(
            JSONObject stats
    ) {

        serverMmr =
                stats.optInt(
                        "mmr",
                        serverMmr
                );

        serverWins =
                stats.optInt(
                        "wins",
                        serverWins
                );

        serverLosses =
                stats.optInt(
                        "losses",
                        serverLosses
                );

        serverCurrentStreak =
                stats.optInt(
                        "current_streak",
                        serverCurrentStreak
                );

        serverBestStreak =
                stats.optInt(
                        "best_streak",
                        serverBestStreak
                );

        // Cache only.
        // Supabase remains authoritative.

        sessionPrefs
                .edit()
                .putInt(
                        "mmr",
                        serverMmr
                )
                .putInt(
                        "wins",
                        serverWins
                )
                .putInt(
                        "losses",
                        serverLosses
                )
                .putInt(
                        "current_streak",
                        serverCurrentStreak
                )
                .putInt(
                        "best_streak",
                        serverBestStreak
                )
                .apply();

        updateStats();
    }

    // ------------------------------------------------
    // UPDATE UI
    // ------------------------------------------------

    private void updateStats() {

        int total =
                serverWins +
                        serverLosses;

        int winRate =
                total == 0
                        ? 0
                        : Math.round(
                        (
                                serverWins *
                                        100f
                        ) /
                                total
                );

        txtMmr.setText(
                "MMR  " +
                        String.format(
                                Locale.getDefault(),
                                "%,d",
                                serverMmr
                        )
        );

        txtProfileStats.setText(
                "WINS: " +
                        serverWins +
                        "     LOSSES: " +
                        serverLosses +
                        "\nWIN RATE: " +
                        winRate +
                        "%" +
                        "     STREAK: " +
                        serverCurrentStreak
        );
    }

    // ------------------------------------------------
    // WIN DIALOG
    // ------------------------------------------------

    private void showWinDialog(
            int mmrChange
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "NETWORK RESTORED!"
                )
                .setMessage(
                        "You completed all 20 rounds!\n\n" +
                                "Correct: " +
                                correct +
                                "/" +
                                TARGET +
                                "\nTime: " +
                                formatTime(
                                        elapsedSeconds
                                ) +
                                "\nMMR: +" +
                                mmrChange +
                                "\nNew rating: " +
                                serverMmr
                )
                .setCancelable(false)
                .setPositiveButton(
                        "PLAY AGAIN",
                        (dialog, which) ->
                                startGame()
                )
                .setNegativeButton(
                        "BACK",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    // ------------------------------------------------
    // LOSS DIALOG
    // ------------------------------------------------

    private void showLossDialog(
            int mmrChange
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "TRANSMISSION LOST"
                )
                .setMessage(
                        "Your network was interrupted.\n\n" +
                                "Correct: " +
                                correct +
                                "/" +
                                TARGET +
                                "\nTime: " +
                                formatTime(
                                        elapsedSeconds
                                ) +
                                "\nMMR: " +
                                mmrChange +
                                "\nNew rating: " +
                                serverMmr
                )
                .setCancelable(false)
                .setPositiveButton(
                        "TRY AGAIN",
                        (dialog, which) ->
                                startGame()
                )
                .setNegativeButton(
                        "BACK",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    // ------------------------------------------------
    // READY DIALOG
    // ------------------------------------------------

    private void showReadyDialog() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "READY, ARCHITECT?"
                )
                .setMessage(
                        "Your starting MMR: " +
                                serverMmr +
                                "\n\nYou have 20 rounds. One mistake ends your run."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "START",
                        (dialog, which) ->
                                beginCountdown()
                )
                .setNegativeButton(
                        "BACK",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    // ------------------------------------------------
    // COUNTDOWN
    // ------------------------------------------------

    private void beginCountdown() {

        final int[] count = {
                3
        };

        countdownDialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "GET READY"
                        )
                        .setMessage(
                                "Starting in 3..."
                        )
                        .setCancelable(false)
                        .create();

        countdownDialog.show();

        countdownRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (
                                isFinishing() ||
                                        isDestroyed()
                        ) {

                            return;
                        }

                        if (
                                count[0] > 0
                        ) {

                            countdownDialog
                                    .setMessage(
                                            "Starting in " +
                                                    count[0] +
                                                    "..."
                                    );

                            count[0]--;

                            handler.postDelayed(
                                    this,
                                    1000
                            );

                        } else {

                            countdownDialog.dismiss();

                            countdownDialog = null;

                            countdownRunnable = null;

                            startGame();
                        }
                    }
                };

        handler.post(
                countdownRunnable
        );
    }

    // ------------------------------------------------
    // HEADERS
    // ------------------------------------------------

    private Map<String, String> buildHeaders() {

        Map<String, String> headers =
                new HashMap<>();

        headers.put(
                "Content-Type",
                "application/json"
        );

        headers.put(
                "apikey",
                ApiConfig.SUPABASE_ANON_KEY
        );

        headers.put(
                "Authorization",
                "Bearer " +
                        getSessionToken()
        );

        return headers;
    }

    // ------------------------------------------------
    // SESSION TOKEN
    // ------------------------------------------------

    private String getSessionToken() {

        String token =
                sessionPrefs.getString(
                        "session_token",
                        ""
                );

        if (token == null) {
            return "";
        }

        return token.trim();
    }

    // ------------------------------------------------
    // SESSION ERROR
    // ------------------------------------------------

    private void showSessionError() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "LOGIN REQUIRED"
                )
                .setMessage(
                        "Your login session is missing or expired. Please log in again."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "OK",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    // ------------------------------------------------
    // DISABLE ANSWERS
    // ------------------------------------------------

    private void disableAnswerButtons() {

        for (
                Button button :
                answerButtons
        ) {

            button.setEnabled(false);
        }
    }

    // ------------------------------------------------
    // TIMER
    // ------------------------------------------------

    private void updateTimer() {

        txtTime.setText(
                formatTime(
                        elapsedSeconds
                )
        );
    }

    private String formatTime(
            int seconds
    ) {

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                seconds / 60,
                seconds % 60
        );
    }


    // ------------------------------------------------
    // GLOBAL FLASHLIGHT SETTING
    // ------------------------------------------------

    private void loadSignalSettings() {
        SharedPreferences prefs =
                getSharedPreferences("MorseSettings", MODE_PRIVATE);

        flashEnabled = prefs.getBoolean("flash_enabled", true);
        int wpm = prefs.getInt("speed", 20);
        flashUnitMs = Math.max(20, 1200 / Math.max(5, wpm));

        if (!flashEnabled) {
            setTorch(false);
        }
    }

    private void playFlashMorse(String code) {
        signalHandler.removeCallbacksAndMessages(null);
        setTorch(false);

        if (!flashEnabled || code == null || code.trim().isEmpty()) {
            return;
        }

        long delay = 0L;

        for (char c : code.toCharArray()) {
            if (c == '.' || c == '-') {
                int duration = c == '.' ? flashUnitMs : flashUnitMs * 3;
                long onAt = delay;
                long offAt = delay + duration;

                signalHandler.postDelayed(() -> setTorch(true), onAt);
                signalHandler.postDelayed(() -> setTorch(false), offAt);

                delay += duration + flashUnitMs;
            } else if (c == ' ') {
                delay += flashUnitMs * 2L;
            }
        }

        signalHandler.postDelayed(() -> setTorch(false), delay + 50L);
    }

    private void setTorch(boolean enabled) {
        if (cameraManager == null || cameraId == null) {
            return;
        }

        try {
            cameraManager.setTorchMode(cameraId, enabled && flashEnabled);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSignalSettings();
    }

    @Override
    protected void onPause() {
        signalHandler.removeCallbacksAndMessages(null);
        setTorch(false);
        super.onPause();
    }

    // ------------------------------------------------
    // DESTROY
    // ------------------------------------------------

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(
                timerRunnable
        );

        signalHandler.removeCallbacksAndMessages(null);
        setTorch(false);

        if (
                countdownRunnable != null
        ) {

            handler.removeCallbacks(
                    countdownRunnable
            );
        }

        if (
                countdownDialog != null &&
                        countdownDialog.isShowing()
        ) {

            countdownDialog.dismiss();
        }

        if (
                requestQueue != null
        ) {

            requestQueue.cancelAll(
                    request ->
                            true
            );
        }

        super.onDestroy();
    }
}