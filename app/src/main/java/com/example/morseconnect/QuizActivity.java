package com.example.morseconnect;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG =
            "QUIZ_ACTIVITY";

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private static final int TOTAL_QUESTIONS = 20;
    private static final int PASS_SCORE = 15;

    private TextView txtQuestion;
    private TextView txtMorseDisplay;
    private TextView txtQuestionProgress;
    private TextView txtScore;
    private TextView txtDifficulty;

    private MaterialButton btnChoice1;
    private MaterialButton btnChoice2;
    private MaterialButton btnChoice3;
    private MaterialButton btnChoice4;
    private MaterialButton btnPlay;
    private MaterialButton btnSelect;

    private View flashIndicator;

    private String correctAnswer;
    private String selectedAnswer;
    private String currentMorse;

    private String mode = "easy";
    private String sessionToken = "";

    private boolean dailyChallenge = false;

    private Map<String, String> morseMap;
    private List<String> keys;

    private final Random random =
            new Random();

    private final Handler handler =
            new Handler();

    private CameraManager cameraManager;
    private String cameraId;

    private int questionNumber = 1;
    private int score = 0;

    private boolean answerSubmitted = false;
    private boolean resultSubmitting = false;

    private int wpm = 20;
    private int frequency = 600;
    private float volume = 0.8f;
    private int unitMs = 60;

    private boolean flashEnabled = true;

    private AudioTrack audioTrack;

    private MaterialButton selectedButton;

    private final int COLOR_CHOICE_BACKGROUND =
            Color.parseColor("#171B27");

    private final int COLOR_CHOICE_BORDER =
            Color.parseColor("#3B3555");

    private final int COLOR_SELECTED_BACKGROUND =
            Color.parseColor("#352A50");

    private final int COLOR_SELECTED_BORDER =
            Color.parseColor("#A987E8");

    private final int COLOR_CORRECT_BACKGROUND =
            Color.parseColor("#17372E");

    private final int COLOR_CORRECT_BORDER =
            Color.parseColor("#67D7A4");

    private final int COLOR_WRONG_BACKGROUND =
            Color.parseColor("#3A2027");

    private final int COLOR_WRONG_BORDER =
            Color.parseColor("#E87575");

    private final int COLOR_TEXT =
            Color.parseColor("#FFFFFF");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_quiz
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        bindViews();
        loadSession();
        loadSettings();

        String receivedMode =
                getIntent()
                        .getStringExtra(
                                "mode"
                        );

        if (
                receivedMode != null &&
                        (
                                receivedMode.equals("easy") ||
                                        receivedMode.equals("medium") ||
                                        receivedMode.equals("hard")
                        )
        ) {

            mode = receivedMode;
        }

        dailyChallenge =
                getIntent()
                        .getBooleanExtra(
                                "daily_challenge",
                                false
                        );

        morseMap =
                getFullMorseMap();

        keys =
                new ArrayList<>(
                        morseMap.keySet()
                );

        cameraManager =
                (CameraManager)
                        getSystemService(
                                CAMERA_SERVICE
                        );

        try {

            cameraId =
                    cameraManager
                            .getCameraIdList()[0];

        } catch (Exception ignored) {
        }

        setupListeners();
        setupBackHandling();

        updateHeader();
        generateQuestion();
    }

    private void bindViews() {

        TextView btnBack =
                findViewById(
                        R.id.btnBack
                );

        txtQuestion =
                findViewById(
                        R.id.txtQuestion
                );

        txtMorseDisplay =
                findViewById(
                        R.id.txtMorseDisplay
                );

        txtQuestionProgress =
                findViewById(
                        R.id.txtQuestionProgress
                );

        txtScore =
                findViewById(
                        R.id.txtScore
                );

        txtDifficulty =
                findViewById(
                        R.id.txtDifficulty
                );

        btnChoice1 =
                findViewById(
                        R.id.btnChoice1
                );

        btnChoice2 =
                findViewById(
                        R.id.btnChoice2
                );

        btnChoice3 =
                findViewById(
                        R.id.btnChoice3
                );

        btnChoice4 =
                findViewById(
                        R.id.btnChoice4
                );

        btnPlay =
                findViewById(
                        R.id.btnPlay
                );

        btnSelect =
                findViewById(
                        R.id.btnSelect
                );

        flashIndicator =
                findViewById(
                        R.id.flashIndicator
                );

        btnBack.setOnClickListener(
                v -> handleExitRequest()
        );
    }

    private void loadSession() {

        SharedPreferences prefs =
                getSharedPreferences(
                        SESSION_PREFS,
                        MODE_PRIVATE
                );

        sessionToken =
                prefs.getString(
                        "session_token",
                        ""
                );

        if (sessionToken == null) {
            sessionToken = "";
        }
    }

    private void setupListeners() {

        btnChoice1.setOnClickListener(
                v -> selectAnswer(
                        btnChoice1
                )
        );

        btnChoice2.setOnClickListener(
                v -> selectAnswer(
                        btnChoice2
                )
        );

        btnChoice3.setOnClickListener(
                v -> selectAnswer(
                        btnChoice3
                )
        );

        btnChoice4.setOnClickListener(
                v -> selectAnswer(
                        btnChoice4
                )
        );

        btnPlay.setOnClickListener(v -> {

            if (!answerSubmitted &&
                    !resultSubmitting) {

                playMorse(
                        currentMorse
                );
            }
        });

        btnSelect.setOnClickListener(v -> {

            if (resultSubmitting) {
                return;
            }

            if (!answerSubmitted) {

                if (selectedAnswer == null) {

                    Toast.makeText(
                            this,
                            "Choose an answer first.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                submitCurrentAnswer();

            } else {

                moveToNextQuestion();
            }
        });
    }

    private void setupBackHandling() {

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(
                                true
                        ) {

                            @Override
                            public void handleOnBackPressed() {

                                handleExitRequest();
                            }
                        }
                );
    }

    private void handleExitRequest() {

        if (resultSubmitting) {
            return;
        }

        if (dailyChallenge) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Quit Daily Challenge?"
                    )
                    .setMessage(
                            "Leaving now will end today's Daily Challenge. " +
                                    "Your current score will be submitted and " +
                                    "you won't be able to retry until tomorrow."
                    )
                    .setNegativeButton(
                            "CONTINUE CHALLENGE",
                            null
                    )
                    .setPositiveButton(
                            "QUIT & SUBMIT",
                            (dialog, which) ->
                                    submitDailyQuit()
                    )
                    .show();

        } else {

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Leave quiz?"
                    )
                    .setMessage(
                            "Your current quiz attempt will not be saved."
                    )
                    .setNegativeButton(
                            "Stay",
                            null
                    )
                    .setPositiveButton(
                            "Leave",
                            (dialog, which) -> {

                                stopSignal();
                                finish();
                            }
                    )
                    .show();
        }
    }

    private void submitDailyQuit() {

        if (resultSubmitting) {
            return;
        }

        resultSubmitting = true;

        stopSignal();
        disableQuizControls();

        btnSelect.setText(
                "SUBMITTING..."
        );

        submitDailyResult(
                "quit"
        );
    }

    private void updateHeader() {

        txtQuestionProgress.setText(
                "Question " +
                        questionNumber +
                        " of " +
                        TOTAL_QUESTIONS
        );

        txtScore.setText(
                "Score " +
                        score
        );

        if (dailyChallenge) {

            txtDifficulty.setText(
                    "DAILY"
            );

        } else {

            switch (mode) {

                case "medium":

                    txtDifficulty.setText(
                            "MEDIUM"
                    );

                    break;

                case "hard":

                    txtDifficulty.setText(
                            "HARD"
                    );

                    break;

                default:

                    txtDifficulty.setText(
                            "EASY"
                    );

                    break;
            }
        }
    }

    private void selectAnswer(
            MaterialButton button
    ) {

        if (answerSubmitted ||
                resultSubmitting) {

            return;
        }

        selectedButton = button;

        resetChoiceStyles();
        styleSelected(button);

        if (mode.equals("hard")) {

            selectedAnswer =
                    button
                            .getText()
                            .toString();

        } else {

            Object tag =
                    button.getTag();

            if (tag == null) {
                return;
            }

            selectedAnswer =
                    tag.toString();

            String selectedMorse =
                    morseMap.get(
                            selectedAnswer
                    );

            playMorse(
                    selectedMorse
            );
        }
    }

    private void submitCurrentAnswer() {

        if (
                selectedAnswer == null ||
                        selectedButton == null
        ) {

            return;
        }

        answerSubmitted = true;

        boolean correct =
                selectedAnswer.equals(
                        correctAnswer
                );

        if (correct) {

            score++;

            styleCorrect(
                    selectedButton
            );

        } else {

            styleWrong(
                    selectedButton
            );

            highlightCorrectAnswer();
        }

        setChoiceButtonsEnabled(
                false
        );

        txtScore.setText(
                "Score " +
                        score
        );

        if (
                questionNumber >=
                        TOTAL_QUESTIONS
        ) {

            btnSelect.setText(
                    "VIEW RESULTS"
            );

        } else {

            btnSelect.setText(
                    "NEXT QUESTION"
            );
        }
    }

    private void moveToNextQuestion() {

        if (
                questionNumber >=
                        TOTAL_QUESTIONS
        ) {

            finishQuiz();
            return;
        }

        questionNumber++;

        generateQuestion();
    }

    private void highlightCorrectAnswer() {

        for (
                MaterialButton button :
                getChoiceButtons()
        ) {

            String answer;

            if (mode.equals("hard")) {

                answer =
                        button
                                .getText()
                                .toString();

            } else {

                Object tag =
                        button.getTag();

                answer =
                        tag == null
                                ? ""
                                : tag.toString();
            }

            if (
                    correctAnswer.equals(
                            answer
                    )
            ) {

                styleCorrect(
                        button
                );

                break;
            }
        }
    }

    private void generateQuestion() {

        handler.removeCallbacksAndMessages(
                null
        );

        stopSignal();

        answerSubmitted = false;
        selectedAnswer = null;
        selectedButton = null;

        setChoiceButtonsEnabled(
                true
        );

        resetChoiceStyles();

        btnSelect.setEnabled(true);

        btnSelect.setText(
                "CHECK ANSWER"
        );

        txtMorseDisplay.setText("");

        updateHeader();

        List<String> pool =
                buildQuestionPool();

        if (pool.size() < 4) {

            Toast.makeText(
                    this,
                    "Not enough quiz questions available.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        String key =
                pool.get(
                        random.nextInt(
                                pool.size()
                        )
                );

        String morse =
                morseMap.get(
                        key
                );

        correctAnswer = key;
        currentMorse = morse;

        List<String> choices =
                new ArrayList<>();

        choices.add(key);

        while (
                choices.size() < 4
        ) {

            String wrong =
                    pool.get(
                            random.nextInt(
                                    pool.size()
                            )
                    );

            if (
                    !choices.contains(
                            wrong
                    )
            ) {

                choices.add(
                        wrong
                );
            }
        }

        Collections.shuffle(
                choices
        );

        MaterialButton[] buttons = {
                btnChoice1,
                btnChoice2,
                btnChoice3,
                btnChoice4
        };

        if (!mode.equals("hard")) {

            txtQuestion.setText(
                    "Which signal represents \"" +
                            key +
                            "\"?"
            );

            for (
                    int i = 0;
                    i < buttons.length;
                    i++
            ) {

                buttons[i].setTag(
                        choices.get(i)
                );

                buttons[i].setText(
                        "CHOICE " +
                                (char) (
                                        'A' + i
                                )
                );
            }

            btnPlay.setVisibility(
                    View.GONE
            );

        } else {

            txtQuestion.setText(
                    "What does this Morse signal represent?"
            );

            for (
                    int i = 0;
                    i < buttons.length;
                    i++
            ) {

                buttons[i].setTag(
                        null
                );

                buttons[i].setText(
                        choices.get(i)
                );
            }

            btnPlay.setVisibility(
                    View.VISIBLE
            );

            playMorse(
                    morse
            );
        }
    }

    private List<String> buildQuestionPool() {

        List<String> pool =
                new ArrayList<>();

        if (mode.equals("easy")) {

            for (String key : keys) {

                if (
                        key.matches(
                                "[A-Z]"
                        )
                ) {

                    pool.add(key);
                }
            }

        } else if (
                mode.equals("medium")
        ) {

            for (String key : keys) {

                if (
                        key.matches(
                                "[0-9]"
                        ) ||
                                isSymbol(key)
                ) {

                    pool.add(key);
                }
            }

        } else {

            pool.addAll(keys);
        }

        return pool;
    }

    private boolean isSymbol(
            String value
    ) {

        return value.equals(".") ||
                value.equals(",") ||
                value.equals("?") ||
                value.equals("!");
    }

    private void finishQuiz() {

        if (resultSubmitting) {
            return;
        }

        resultSubmitting = true;

        stopSignal();
        disableQuizControls();

        btnSelect.setText(
                "SAVING RESULT..."
        );

        if (dailyChallenge) {

            submitDailyResult(
                    "finish"
            );

        } else {

            saveQuizResult();
        }
    }

    private void disableQuizControls() {

        btnChoice1.setEnabled(false);
        btnChoice2.setEnabled(false);
        btnChoice3.setEnabled(false);
        btnChoice4.setEnabled(false);

        btnPlay.setEnabled(false);
        btnSelect.setEnabled(false);
    }

    private void saveQuizResult() {

        if (
                sessionToken
                        .trim()
                        .isEmpty()
        ) {

            resultSubmitting = false;

            showQuizResultDialog(
                    false,
                    "Your result could not be synced because there is no active login session."
            );

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "quiz-progress";

        JSONObject body =
                new JSONObject();

        try {

            body.put(
                    "mode",
                    mode
            );

            body.put(
                    "score",
                    score
            );

        } catch (
                JSONException e
        ) {

            resultSubmitting = false;

            showQuizResultDialog(
                    false,
                    "Unable to prepare quiz result."
            );

            return;
        }

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            resultSubmitting = false;

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                showQuizResultDialog(
                                        false,
                                        response.optString(
                                                "message",
                                                "Unable to save quiz result."
                                        )
                                );

                                return;
                            }

                            JSONObject result =
                                    response.optJSONObject(
                                            "result"
                                    );

                            boolean passed =
                                    result != null
                                            ? result.optBoolean(
                                            "passed",
                                            score >=
                                            PASS_SCORE
                                    )
                                            : score >=
                                              PASS_SCORE;

                            String message;

                            if (passed) {

                                if (
                                        mode.equals(
                                                "easy"
                                        )
                                ) {

                                    message =
                                            "Easy completed! Medium is now unlocked.";

                                } else if (
                                        mode.equals(
                                                "medium"
                                        )
                                ) {

                                    message =
                                            "Medium completed! Hard is now unlocked.";

                                } else {

                                    message =
                                            "Hard completed!";
                                }

                            } else {

                                message =
                                        "You need at least 15/20 to complete this difficulty.";
                            }

                            showQuizResultDialog(
                                    true,
                                    message
                            );
                        },

                        error -> {

                            resultSubmitting = false;

                            Log.e(
                                    TAG,
                                    "Unable to save quiz result",
                                    error
                            );

                            String message =
                                    "Your score could not be synced. Please try again.";

                            if (
                                    error.networkResponse != null &&
                                            error.networkResponse.statusCode == 401
                            ) {

                                message =
                                        "Your session has expired. Please log in again.";
                            }

                            showQuizResultDialog(
                                    false,
                                    message
                            );
                        }
                ) {

                    @Override
                    public Map<String, String>
                    getHeaders() {

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

        Volley
                .newRequestQueue(this)
                .add(request);
    }

    private void submitDailyResult(
            String action
    ) {

        if (
                sessionToken
                        .trim()
                        .isEmpty()
        ) {

            resultSubmitting = false;

            showDailySaveFailure(
                    "There is no active login session."
            );

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "daily-challenge";

        JSONObject body =
                new JSONObject();

        try {

            body.put(
                    "action",
                    action
            );

            body.put(
                    "score",
                    score
            );

        } catch (
                JSONException e
        ) {

            resultSubmitting = false;

            showDailySaveFailure(
                    "Unable to prepare Daily Challenge result."
            );

            return;
        }

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            resultSubmitting = false;

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                showDailySaveFailure(
                                        response.optString(
                                                "message",
                                                "Unable to save Daily Challenge."
                                        )
                                );

                                return;
                            }

                            JSONObject result =
                                    response.optJSONObject(
                                            "result"
                                    );

                            int mmrEarned = 0;

                            if (result != null) {

                                mmrEarned =
                                        result.optInt(
                                                "mmr_earned",
                                                0
                                        );
                            }

                            showDailyResultDialog(
                                    action,
                                    mmrEarned
                            );
                        },

                        error -> {

                            resultSubmitting = false;

                            Log.e(
                                    TAG,
                                    "Daily result save failed",
                                    error
                            );

                            String message =
                                    "Unable to save Daily Challenge.";

                            if (
                                    error.networkResponse != null
                            ) {

                                int status =
                                        error
                                                .networkResponse
                                                .statusCode;

                                if (status == 401) {

                                    message =
                                            "Your session has expired. Please log in again.";

                                } else if (
                                        status == 409
                                ) {

                                    message =
                                            "Today's Daily Challenge has already been finalized.";
                                }
                            }

                            showDailySaveFailure(
                                    message
                            );
                        }
                ) {

                    @Override
                    public Map<String, String>
                    getHeaders() {

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

        Volley
                .newRequestQueue(this)
                .add(request);
    }

    private Map<String, String>
    buildHeaders() {

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
                        sessionToken
        );

        return headers;
    }

    private void showQuizResultDialog(
            boolean saved,
            String message
    ) {

        boolean passed =
                score >= PASS_SCORE;

        String title =
                passed
                        ? "Quiz Complete"
                        : "Quiz Finished";

        String finalMessage =
                "Score: " +
                        score +
                        "/" +
                        TOTAL_QUESTIONS +
                        "\n\n" +
                        message;

        if (!saved) {

            finalMessage +=
                    "\n\nThe server did not save this attempt.";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        finalMessage
                )
                .setCancelable(false)
                .setPositiveButton(
                        "BACK TO DIFFICULTIES",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void showDailyResultDialog(
            String action,
            int mmrEarned
    ) {

        String message;

        if (
                action.equals(
                        "quit"
                )
        ) {

            message =
                    "Today's challenge ended early.";

        } else {

            message =
                    "Today's transmission is complete.";
        }

        String finalMessage =
                "Score: " +
                        score +
                        "/" +
                        TOTAL_QUESTIONS +
                        "\n\nMMR earned: +" +
                        mmrEarned +
                        "\n\n" +
                        message;

        new AlertDialog.Builder(this)
                .setTitle(
                        "Daily Complete"
                )
                .setMessage(
                        finalMessage
                )
                .setCancelable(false)
                .setPositiveButton(
                        "BACK TO DAILY",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void showDailySaveFailure(
            String message
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Daily Challenge"
                )
                .setMessage(
                        message +
                                "\n\nToday's attempt has already been reserved, so restarting the app will not create another attempt."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "BACK TO DAILY",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private List<MaterialButton>
    getChoiceButtons() {

        return Arrays.asList(
                btnChoice1,
                btnChoice2,
                btnChoice3,
                btnChoice4
        );
    }

    private void resetChoiceStyles() {

        for (
                MaterialButton button :
                getChoiceButtons()
        ) {

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            COLOR_CHOICE_BACKGROUND
                    )
            );

            button.setStrokeColor(
                    ColorStateList.valueOf(
                            COLOR_CHOICE_BORDER
                    )
            );

            button.setTextColor(
                    COLOR_TEXT
            );
        }
    }

    private void styleSelected(
            MaterialButton button
    ) {

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        COLOR_SELECTED_BACKGROUND
                )
        );

        button.setStrokeColor(
                ColorStateList.valueOf(
                        COLOR_SELECTED_BORDER
                )
        );

        button.setTextColor(
                COLOR_TEXT
        );
    }

    private void styleCorrect(
            MaterialButton button
    ) {

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        COLOR_CORRECT_BACKGROUND
                )
        );

        button.setStrokeColor(
                ColorStateList.valueOf(
                        COLOR_CORRECT_BORDER
                )
        );

        button.setTextColor(
                Color.WHITE
        );
    }

    private void styleWrong(
            MaterialButton button
    ) {

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        COLOR_WRONG_BACKGROUND
                )
        );

        button.setStrokeColor(
                ColorStateList.valueOf(
                        COLOR_WRONG_BORDER
                )
        );

        button.setTextColor(
                Color.WHITE
        );
    }

    private void setChoiceButtonsEnabled(
            boolean enabled
    ) {

        for (
                MaterialButton button :
                getChoiceButtons()
        ) {

            button.setEnabled(
                    enabled
            );

            button.setAlpha(
                    1.0f
            );
        }
    }

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

        int volumeInteger =
                prefs.getInt(
                        "volume",
                        80
                );

        volume =
                volumeInteger /
                        100f;

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

        if (wpm <= 0) {
            wpm = 20;
        }

        unitMs =
                1200 / wpm;
    }

    private void playMorse(
            String code
    ) {

        handler.removeCallbacksAndMessages(
                null
        );

        stopSignal();

        txtMorseDisplay.setText("");

        if (
                code == null ||
                        code.trim().isEmpty()
        ) {

            return;
        }

        byte[] audioData =
                buildMorseAudio(
                        code
                );

        if (
                audioData.length == 0
        ) {

            return;
        }

        try {

            audioTrack =
                    new AudioTrack(
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
            return;
        }

        long delay = 0;

        for (
                char c :
                code.toCharArray()
        ) {

            if (
                    c == '.' ||
                            c == '-'
            ) {

                final char mark = c;

                int duration =
                        c == '.'
                                ? unitMs
                                : unitMs * 3;

                handler.postDelayed(
                        () -> {

                            txtMorseDisplay.append(
                                    String.valueOf(
                                            mark
                                    )
                            );

                            setSignalVisual(
                                    true
                            );
                        },
                        delay
                );

                handler.postDelayed(
                        () ->
                                setSignalVisual(
                                        false
                                ),
                        delay +
                                duration
                );

                delay +=
                        duration +
                                unitMs;

            } else if (
                    c == ' '
            ) {

                final long spaceDelay =
                        delay;

                handler.postDelayed(
                        () ->
                                txtMorseDisplay
                                        .append(
                                                " "
                                        ),
                        spaceDelay
                );

                delay +=
                        unitMs * 2L;
            }
        }

        handler.postDelayed(
                () -> {

                    setSignalVisual(
                            false
                    );

                    releaseAudioTrack();
                },
                delay + 80L
        );
    }

    private byte[] buildMorseAudio(
            String code
    ) {

        final int sampleRate =
                44100;

        int totalSamples = 0;

        for (
                char c :
                code.toCharArray()
        ) {

            if (c == '.') {

                totalSamples +=
                        msToSamples(
                                unitMs * 2,
                                sampleRate
                        );

            } else if (
                    c == '-'
            ) {

                totalSamples +=
                        msToSamples(
                                unitMs * 4,
                                sampleRate
                        );

            } else if (
                    c == ' '
            ) {

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

        for (
                char c :
                code.toCharArray()
        ) {

            if (
                    c == '.' ||
                            c == '-'
            ) {

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

            } else if (
                    c == ' '
            ) {

                position +=
                        msToSamples(
                                unitMs * 2,
                                sampleRate
                        );
            }
        }

        byte[] data =
                new byte[
                        pcm.length * 2
                        ];

        int index = 0;

        for (
                short value :
                pcm
        ) {

            data[index++] =
                    (byte) (
                            value &
                                    0xFF
                    );

            data[index++] =
                    (byte) (
                            (
                                    value >>
                                            8
                            ) &
                                    0xFF
                    );
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
                                (
                                        ms /
                                                1000.0
                                )
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
                        start + i <
                                buffer.length;
                i++
        ) {

            double envelope =
                    1.0;

            if (
                    i <
                            fadeSamples
            ) {

                envelope =
                        (double) i /
                                fadeSamples;

            } else if (
                    i >=
                            length -
                                    fadeSamples
            ) {

                envelope =
                        (double) (
                                length -
                                        i -
                                        1
                        ) /
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
                                    phaseStep *
                                            i
                            ) *
                                    32767.0 *
                                    volume *
                                    envelope
                    );
        }
    }

    private void setSignalVisual(
            boolean active
    ) {

        if (
                cameraManager != null &&
                        cameraId != null
        ) {

            try {

                cameraManager
                        .setTorchMode(
                                cameraId,
                                active &&
                                        flashEnabled
                        );

            } catch (
                    Exception ignored
            ) {
            }
        }

        flashIndicator
                .setBackgroundResource(
                        active
                                ? R.drawable.indicator_on
                                : R.drawable.indicator_off
                );
    }

    private void releaseAudioTrack() {

        if (
                audioTrack != null
        ) {

            try {

                audioTrack.stop();

            } catch (
                    Exception ignored
            ) {
            }

            try {

                audioTrack.release();

            } catch (
                    Exception ignored
            ) {
            }

            audioTrack = null;
        }
    }

    private void stopSignal() {

        releaseAudioTrack();

        setSignalVisual(
                false
        );
    }

    private Map<String, String>
    getFullMorseMap() {

        Map<String, String> map =
                new HashMap<>();

        map.put("A", ".-");
        map.put("B", "-...");
        map.put("C", "-.-.");
        map.put("D", "-..");
        map.put("E", ".");
        map.put("F", "..-.");
        map.put("G", "--.");
        map.put("H", "....");
        map.put("I", "..");
        map.put("J", ".---");
        map.put("K", "-.-");
        map.put("L", ".-..");
        map.put("M", "--");
        map.put("N", "-.");
        map.put("O", "---");
        map.put("P", ".--.");
        map.put("Q", "--.-");
        map.put("R", ".-.");
        map.put("S", "...");
        map.put("T", "-");

        map.put("1", ".----");
        map.put("2", "..---");
        map.put("3", "...--");
        map.put("4", "....-");
        map.put("5", ".....");

        map.put(".", ".-.-.-");
        map.put(",", "--..--");
        map.put("?", "..--..");
        map.put("!", "-.-.--");

        map.put(
                "SOS",
                "... --- ..."
        );

        map.put(
                "HELLO",
                ".... . .-.. .-.. ---"
        );

        map.put(
                "AR",
                ".-.-."
        );

        map.put(
                "SK",
                "...-.-"
        );

        map.put(
                "BT",
                "-...-"
        );

        return map;
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler
                .removeCallbacksAndMessages(
                        null
                );

        stopSignal();
    }

    @Override
    protected void onDestroy() {

        handler
                .removeCallbacksAndMessages(
                        null
                );

        stopSignal();

        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {

        handleExitRequest();

        return true;
    }
}