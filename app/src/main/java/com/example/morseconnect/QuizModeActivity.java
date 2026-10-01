package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class QuizModeActivity extends AppCompatActivity {

    private static final String TAG = "QUIZ_PROGRESS";

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private MaterialButton btnEasy;
    private MaterialButton btnMedium;
    private MaterialButton btnHard;

    private TextView txtEasyStatus;
    private TextView txtMediumStatus;
    private TextView txtHardStatus;
    private TextView txtProgressMessage;

    private boolean mediumUnlocked = false;
    private boolean hardUnlocked = false;

    private boolean easyCompleted = false;
    private boolean mediumCompleted = false;
    private boolean hardCompleted = false;

    private int easyBestScore = 0;
    private int mediumBestScore = 0;
    private int hardBestScore = 0;

    private String sessionToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_mode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        bindViews();
        loadSession();
        setupButtons();

        // Start locked until the server confirms progress.
        showLoadingState();
    }

    private void bindViews() {

        TextView btnBack =
                findViewById(R.id.btnBack);

        btnEasy =
                findViewById(R.id.btnEasy);

        btnMedium =
                findViewById(R.id.btnMedium);

        btnHard =
                findViewById(R.id.btnHard);

        txtEasyStatus =
                findViewById(R.id.txtEasyStatus);

        txtMediumStatus =
                findViewById(R.id.txtMediumStatus);

        txtHardStatus =
                findViewById(R.id.txtHardStatus);

        txtProgressMessage =
                findViewById(R.id.txtProgressMessage);

        btnBack.setOnClickListener(v -> finish());
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

        boolean loggedIn =
                prefs.getBoolean(
                        "logged_in",
                        false
                );

        boolean offlineMode =
                prefs.getBoolean(
                        "offline_mode",
                        false
                );

        if (!loggedIn ||
                offlineMode ||
                sessionToken == null ||
                sessionToken.trim().isEmpty()) {

            sessionToken = "";
        }
    }

    private void setupButtons() {

        btnEasy.setOnClickListener(v -> {

            if (!btnEasy.isEnabled()) {
                return;
            }

            startQuiz("easy");
        });

        btnMedium.setOnClickListener(v -> {

            if (!mediumUnlocked) {

                Toast.makeText(
                        this,
                        "Complete Easy with at least 15/20 first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            startQuiz("medium");
        });

        btnHard.setOnClickListener(v -> {

            if (!hardUnlocked) {

                Toast.makeText(
                        this,
                        "Complete Medium with at least 15/20 first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            startQuiz("hard");
        });
    }

    private void startQuiz(String mode) {

        Intent intent =
                new Intent(
                        this,
                        QuizActivity.class
                );

        intent.putExtra(
                "mode",
                mode
        );

        intent.putExtra(
                "daily_challenge",
                false
        );

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionToken == null ||
                sessionToken.trim().isEmpty()) {

            showAuthenticationError();
            return;
        }

        loadQuizProgress();
    }

    private void showLoadingState() {

        btnEasy.setEnabled(false);
        btnMedium.setEnabled(false);
        btnHard.setEnabled(false);

        txtEasyStatus.setText(
                "Checking progress..."
        );

        txtMediumStatus.setText(
                "Checking progress..."
        );

        txtHardStatus.setText(
                "Checking progress..."
        );

        txtProgressMessage.setText(
                "Syncing your quiz progress..."
        );

        setButtonLocked(btnEasy);
        setButtonLocked(btnMedium);
        setButtonLocked(btnHard);
    }

    private void loadQuizProgress() {

        showLoadingState();

        String url =
                ApiConfig.BASE_URL +
                        "quiz-progress";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                showLoadError(
                                        response.optString(
                                                "message",
                                                "Unable to load quiz progress"
                                        )
                                );

                                return;
                            }

                            JSONObject progress =
                                    response.optJSONObject(
                                            "progress"
                                    );

                            if (progress == null) {

                                showLoadError(
                                        "Invalid quiz progress response"
                                );

                                return;
                            }

                            easyCompleted =
                                    progress.optBoolean(
                                            "easy_completed",
                                            false
                                    );

                            easyBestScore =
                                    progress.optInt(
                                            "easy_best_score",
                                            0
                                    );

                            mediumUnlocked =
                                    progress.optBoolean(
                                            "medium_unlocked",
                                            false
                                    );

                            mediumCompleted =
                                    progress.optBoolean(
                                            "medium_completed",
                                            false
                                    );

                            mediumBestScore =
                                    progress.optInt(
                                            "medium_best_score",
                                            0
                                    );

                            hardUnlocked =
                                    progress.optBoolean(
                                            "hard_unlocked",
                                            false
                                    );

                            hardCompleted =
                                    progress.optBoolean(
                                            "hard_completed",
                                            false
                                    );

                            hardBestScore =
                                    progress.optInt(
                                            "hard_best_score",
                                            0
                                    );

                            updateDifficultyUI();
                        },

                        error -> {

                            Log.e(
                                    TAG,
                                    "Unable to load progress",
                                    error
                            );

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                if (statusCode == 401) {

                                    showLoadError(
                                            "Your session has expired. Please log in again."
                                    );

                                    return;
                                }
                            }

                            showLoadError(
                                    "Unable to sync quiz progress."
                            );
                        }
                ) {

                    @Override
                    public Map<String, String> getHeaders() {

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
                                "Bearer " + sessionToken
                        );

                        return headers;
                    }
                };

        request.setRetryPolicy(
                new DefaultRetryPolicy(
                        15000,
                        0,
                        1.0f
                )
        );

        RequestQueue queue =
                Volley.newRequestQueue(this);

        queue.add(request);
    }

    private void updateDifficultyUI() {

        // -----------------------------------------
        // EASY
        // -----------------------------------------

        btnEasy.setEnabled(true);
        setButtonUnlocked(btnEasy);

        if (easyCompleted) {

            txtEasyStatus.setText(
                    "Completed  •  Best " +
                            easyBestScore +
                            "/20"
            );

            txtEasyStatus.setTextColor(
                    Color.parseColor("#67D7A4")
            );

        } else {

            txtEasyStatus.setText(
                    easyBestScore > 0
                            ? "Best " +
                              easyBestScore +
                              "/20  •  Need 15/20"
                            : "Available  •  Need 15/20"
            );

            txtEasyStatus.setTextColor(
                    Color.parseColor("#AAB4C8")
            );
        }

        // -----------------------------------------
        // MEDIUM
        // -----------------------------------------

        if (mediumUnlocked) {

            btnMedium.setEnabled(true);
            setButtonUnlocked(btnMedium);

            if (mediumCompleted) {

                txtMediumStatus.setText(
                        "Completed  •  Best " +
                                mediumBestScore +
                                "/20"
                );

                txtMediumStatus.setTextColor(
                        Color.parseColor("#67D7A4")
                );

            } else {

                txtMediumStatus.setText(
                        mediumBestScore > 0
                                ? "Best " +
                                  mediumBestScore +
                                  "/20  •  Need 15/20"
                                : "Unlocked  •  Need 15/20"
                );

                txtMediumStatus.setTextColor(
                        Color.parseColor("#A987E8")
                );
            }

        } else {

            btnMedium.setEnabled(false);
            setButtonLocked(btnMedium);

            txtMediumStatus.setText(
                    "Locked  •  Complete Easy first"
            );

            txtMediumStatus.setTextColor(
                    Color.parseColor("#77849A")
            );
        }

        // -----------------------------------------
        // HARD
        // -----------------------------------------

        if (hardUnlocked) {

            btnHard.setEnabled(true);
            setButtonUnlocked(btnHard);

            if (hardCompleted) {

                txtHardStatus.setText(
                        "Completed  •  Best " +
                                hardBestScore +
                                "/20"
                );

                txtHardStatus.setTextColor(
                        Color.parseColor("#67D7A4")
                );

            } else {

                txtHardStatus.setText(
                        hardBestScore > 0
                                ? "Best " +
                                  hardBestScore +
                                  "/20  •  Need 15/20"
                                : "Unlocked  •  Need 15/20"
                );

                txtHardStatus.setTextColor(
                        Color.parseColor("#A987E8")
                );
            }

        } else {

            btnHard.setEnabled(false);
            setButtonLocked(btnHard);

            txtHardStatus.setText(
                    "Locked  •  Complete Medium first"
            );

            txtHardStatus.setTextColor(
                    Color.parseColor("#77849A")
            );
        }

        // -----------------------------------------
        // PROGRESS MESSAGE
        // -----------------------------------------

        if (hardCompleted) {

            txtProgressMessage.setText(
                    "All quiz difficulties completed."
            );

        } else if (hardUnlocked) {

            txtProgressMessage.setText(
                    "Hard unlocked. Complete it with 15/20 or better."
            );

        } else if (mediumUnlocked) {

            txtProgressMessage.setText(
                    "Medium unlocked. Score 15/20 to unlock Hard."
            );

        } else {

            txtProgressMessage.setText(
                    "Score 15/20 in Easy to unlock Medium."
            );
        }
    }

    private void setButtonUnlocked(
            MaterialButton button
    ) {

        button.setAlpha(1.0f);

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#2A2142")
                )
        );

        button.setStrokeColor(
                ColorStateList.valueOf(
                        Color.parseColor("#7056A8")
                )
        );

        button.setTextColor(
                Color.WHITE
        );
    }

    private void setButtonLocked(
            MaterialButton button
    ) {

        button.setAlpha(0.45f);

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#171A24")
                )
        );

        button.setStrokeColor(
                ColorStateList.valueOf(
                        Color.parseColor("#293140")
                )
        );

        button.setTextColor(
                Color.parseColor("#77849A")
        );
    }

    private void showLoadError(String message) {

        btnEasy.setEnabled(false);
        btnMedium.setEnabled(false);
        btnHard.setEnabled(false);

        setButtonLocked(btnEasy);
        setButtonLocked(btnMedium);
        setButtonLocked(btnHard);

        txtEasyStatus.setText(
                "Unable to verify progress"
        );

        txtMediumStatus.setText(
                "Unavailable"
        );

        txtHardStatus.setText(
                "Unavailable"
        );

        txtProgressMessage.setText(
                message
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showAuthenticationError() {

        showLoadError(
                "Please log in to use Quiz Mode."
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}