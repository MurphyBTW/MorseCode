package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DailyChallengeActivity extends AppCompatActivity {

    private static final String TAG =
            "DAILY_CHALLENGE";

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private MaterialButton btnStartDaily;

    private TextView txtDailyStatus;
    private TextView txtDailyScore;
    private TextView txtDailyReward;
    private TextView txtDailyDate;

    private String sessionToken = "";

    private boolean completedToday = false;
    private boolean attemptUsed = false;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_daily_challenge
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        bindViews();
        loadSession();

        btnStartDaily.setOnClickListener(v -> {

            if (loading) {
                return;
            }

            if (completedToday ||
                    attemptUsed) {

                Toast.makeText(
                        this,
                        "Today's Daily Challenge has already been used.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            reserveDailyAttempt();
        });
    }

    private void bindViews() {

        TextView btnBack =
                findViewById(R.id.btnBack);

        btnStartDaily =
                findViewById(
                        R.id.btnStartDaily
                );

        txtDailyStatus =
                findViewById(
                        R.id.txtDailyStatus
                );

        txtDailyScore =
                findViewById(
                        R.id.txtDailyScore
                );

        txtDailyReward =
                findViewById(
                        R.id.txtDailyReward
                );

        txtDailyDate =
                findViewById(
                        R.id.txtDailyDate
                );

        btnBack.setOnClickListener(
                v -> finish()
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

    @Override
    protected void onResume() {
        super.onResume();

        loadSession();

        if (sessionToken.isEmpty()) {

            showUnavailable(
                    "Please log in to use Daily Challenge."
            );

            return;
        }

        loadDailyStatus();
    }

    private void loadDailyStatus() {

        loading = true;

        btnStartDaily.setEnabled(false);
        btnStartDaily.setText(
                "CHECKING..."
        );

        txtDailyStatus.setText(
                "Syncing today's transmission..."
        );

        txtDailyScore.setText(
                "-- / 20"
        );

        txtDailyReward.setText(
                "+-- MMR"
        );

        txtDailyDate.setText(
                "Philippine Time"
        );

        String url =
                ApiConfig.BASE_URL +
                        "daily-challenge";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            loading = false;

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                showUnavailable(
                                        response.optString(
                                                "message",
                                                "Unable to load Daily Challenge."
                                        )
                                );

                                return;
                            }

                            String date =
                                    response.optString(
                                            "challenge_date",
                                            ""
                                    );

                            if (!date.isEmpty()) {

                                txtDailyDate.setText(
                                        date +
                                                " • Philippine Time"
                                );
                            }

                            attemptUsed =
                                    response.optBoolean(
                                            "attempt_started",
                                            false
                                    );

                            completedToday =
                                    response.optBoolean(
                                            "completed_today",
                                            false
                                    );

                            boolean abandoned =
                                    response.optBoolean(
                                            "abandoned",
                                            false
                                    );

                            JSONObject result =
                                    response.optJSONObject(
                                            "result"
                                    );

                            if (completedToday) {

                                int savedScore = 0;
                                int reward = 0;

                                if (result != null) {

                                    savedScore =
                                            result.optInt(
                                                    "score",
                                                    0
                                            );

                                    reward =
                                            result.optInt(
                                                    "mmr_earned",
                                                    0
                                            );
                                }

                                showCompleted(
                                        savedScore,
                                        reward
                                );

                                return;
                            }

                            if (attemptUsed ||
                                    abandoned) {

                                showUsedAttempt();
                                return;
                            }

                            showAvailable();
                        },

                        error -> {

                            loading = false;

                            Log.e(
                                    TAG,
                                    "Daily status failed",
                                    error
                            );

                            if (
                                    error.networkResponse != null &&
                                            error.networkResponse.statusCode == 401
                            ) {

                                showUnavailable(
                                        "Your session has expired. Please log in again."
                                );

                                return;
                            }

                            showUnavailable(
                                    "Unable to sync today's challenge."
                            );
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

        RequestQueue queue =
                Volley.newRequestQueue(this);

        queue.add(request);
    }

    private void reserveDailyAttempt() {

        if (loading) {
            return;
        }

        loading = true;

        btnStartDaily.setEnabled(false);
        btnStartDaily.setText(
                "STARTING..."
        );

        String url =
                ApiConfig.BASE_URL +
                        "daily-challenge";

        JSONObject body =
                new JSONObject();

        try {

            body.put(
                    "action",
                    "start"
            );

        } catch (JSONException e) {

            loading = false;
            showAvailable();

            Toast.makeText(
                    this,
                    "Unable to start Daily Challenge.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            loading = false;

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                loadDailyStatus();
                                return;
                            }

                            attemptUsed = true;

                            openDailyQuiz();
                        },

                        error -> {

                            loading = false;

                            Log.e(
                                    TAG,
                                    "Unable to reserve Daily Challenge",
                                    error
                            );

                            if (
                                    error.networkResponse != null &&
                                            error.networkResponse.statusCode == 409
                            ) {

                                Toast.makeText(
                                        this,
                                        "Today's attempt has already been used.",
                                        Toast.LENGTH_LONG
                                ).show();

                                loadDailyStatus();
                                return;
                            }

                            btnStartDaily.setEnabled(true);

                            btnStartDaily.setText(
                                    "START DAILY CHALLENGE"
                            );

                            Toast.makeText(
                                    this,
                                    "Unable to start Daily Challenge.",
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

        Volley
                .newRequestQueue(this)
                .add(request);
    }

    private void openDailyQuiz() {

        Intent intent =
                new Intent(
                        DailyChallengeActivity.this,
                        QuizActivity.class
                );

        intent.putExtra(
                "mode",
                "easy"
        );

        intent.putExtra(
                "daily_challenge",
                true
        );

        startActivity(intent);
    }

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
                        sessionToken
        );

        return headers;
    }

    private void showAvailable() {

        attemptUsed = false;
        completedToday = false;

        txtDailyStatus.setText(
                "Today's challenge is ready."
        );

        txtDailyScore.setText(
                "20 Questions"
        );

        txtDailyReward.setText(
                "Up to +30 MMR"
        );

        btnStartDaily.setEnabled(true);
        btnStartDaily.setAlpha(1.0f);

        btnStartDaily.setText(
                "START DAILY CHALLENGE"
        );
    }

    private void showCompleted(
            int savedScore,
            int reward
    ) {

        attemptUsed = true;
        completedToday = true;

        txtDailyStatus.setText(
                "Transmission completed for today."
        );

        txtDailyScore.setText(
                savedScore +
                        " / 20"
        );

        txtDailyReward.setText(
                "+" +
                        reward +
                        " MMR"
        );

        btnStartDaily.setEnabled(false);
        btnStartDaily.setAlpha(0.45f);

        btnStartDaily.setText(
                "COMPLETED TODAY"
        );
    }

    private void showUsedAttempt() {

        attemptUsed = true;
        completedToday = false;

        txtDailyStatus.setText(
                "Today's attempt was already started and cannot be retried."
        );

        txtDailyScore.setText(
                "Attempt Used"
        );

        txtDailyReward.setText(
                "+0 MMR"
        );

        btnStartDaily.setEnabled(false);
        btnStartDaily.setAlpha(0.45f);

        btnStartDaily.setText(
                "ATTEMPT USED"
        );
    }

    private void showUnavailable(
            String message
    ) {

        attemptUsed = true;

        txtDailyStatus.setText(
                message
        );

        txtDailyScore.setText(
                "-- / 20"
        );

        txtDailyReward.setText(
                "+-- MMR"
        );

        btnStartDaily.setEnabled(false);
        btnStartDaily.setAlpha(0.45f);

        btnStartDaily.setText(
                "UNAVAILABLE"
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}