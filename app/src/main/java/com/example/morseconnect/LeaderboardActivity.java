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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {

    private static final String TAG =
            "LEADERBOARD";

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private TextView txtRankEmblem;
    private TextView txtRank;
    private TextView txtMmr;
    private TextView txtWins;
    private TextView txtLosses;
    private TextView txtWinRate;
    private TextView txtCurrentStreak;
    private TextView txtBestStreak;
    private TextView txtBestTime;

    private SharedPreferences sessionPrefs;

    private RequestQueue requestQueue;

    private boolean initialRequestSent = false;

    // ------------------------------------------------
    // ON CREATE
    // ------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_leaderboard
        );

        sessionPrefs =
                getSharedPreferences(
                        SESSION_PREFS,
                        MODE_PRIVATE
                );

        requestQueue =
                Volley.newRequestQueue(this);

        // ------------------------------------------------
        // VIEWS
        // ------------------------------------------------

        txtRankEmblem =
                findViewById(
                        R.id.txtRankEmblem
                );

        txtRank =
                findViewById(
                        R.id.txtRank
                );

        txtMmr =
                findViewById(
                        R.id.txtMmr
                );

        txtWins =
                findViewById(
                        R.id.txtWins
                );

        txtLosses =
                findViewById(
                        R.id.txtLosses
                );

        txtWinRate =
                findViewById(
                        R.id.txtWinRate
                );

        txtCurrentStreak =
                findViewById(
                        R.id.txtCurrentStreak
                );

        txtBestStreak =
                findViewById(
                        R.id.txtBestStreak
                );

        txtBestTime =
                findViewById(
                        R.id.txtBestTime
                );

        // ------------------------------------------------
        // NAVIGATION
        // ------------------------------------------------

        findViewById(
                R.id.btnHome
        ).setOnClickListener(
                v -> openActivity(
                        MainActivity.class
                )
        );

        findViewById(
                R.id.btnProfile
        ).setOnClickListener(
                v -> openActivity(
                        ProfileActivity.class
                )
        );

        findViewById(
                R.id.btnSettingsTab
        ).setOnClickListener(
                v -> openActivity(
                        SettingsActivity.class
                )
        );

        // ------------------------------------------------
        // CACHED VALUES
        // ------------------------------------------------

        loadCachedStats();

        // ------------------------------------------------
        // SERVER VALUES
        // ------------------------------------------------

        loadStatsFromServer();

        initialRequestSent =
                true;
    }

    // ------------------------------------------------
    // ON RESUME
    // ------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        if (
                initialRequestSent
        ) {

            loadStatsFromServer();
        }
    }

    // ------------------------------------------------
    // CACHED STATS
    // ------------------------------------------------

    private void loadCachedStats() {

        int mmr =
                sessionPrefs.getInt(
                        "mmr",
                        1000
                );

        int wins =
                sessionPrefs.getInt(
                        "wins",
                        0
                );

        int losses =
                sessionPrefs.getInt(
                        "losses",
                        0
                );

        int currentStreak =
                sessionPrefs.getInt(
                        "current_streak",
                        0
                );

        int bestStreak =
                sessionPrefs.getInt(
                        "best_streak",
                        0
                );

        int bestTime =
                sessionPrefs.getInt(
                        "best_time_seconds",
                        -1
                );

        double winRate =
                calculateWinRate(
                        wins,
                        losses
                );

        updateStatsUI(
                mmr,
                wins,
                losses,
                winRate,
                currentStreak,
                bestStreak,
                bestTime
        );
    }

    // ------------------------------------------------
    // LOAD FROM SERVER
    // ------------------------------------------------

    private void loadStatsFromServer() {

        String sessionToken =
                getSessionToken();

        if (
                sessionToken.isEmpty()
        ) {

            handleInvalidSession(
                    "Please log in again."
            );

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

                        response ->
                                handleStatsResponse(
                                        response
                                ),

                        error -> {

                            Log.e(
                                    TAG,
                                    "Stats request failed",
                                    error
                            );

                            if (
                                    error.networkResponse ==
                                            null
                            ) {

                                Toast.makeText(
                                        LeaderboardActivity.this,
                                        "Unable to refresh stats. Showing saved values.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            int statusCode =
                                    error
                                            .networkResponse
                                            .statusCode;

                            if (
                                    statusCode ==
                                            401
                            ) {

                                handleInvalidSession(
                                        "Your login session is no longer valid."
                                );

                            } else {

                                Toast.makeText(
                                        LeaderboardActivity.this,
                                        "Unable to load competitive stats",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                ) {

                    @Override
                    public Map<String, String>
                    getHeaders() {

                        Map<String, String>
                                headers =
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
                };

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
    // HANDLE RESPONSE
    // ------------------------------------------------

    private void handleStatsResponse(
            JSONObject response
    ) {

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

            if (
                    stats ==
                            null
            ) {

                Toast.makeText(
                        this,
                        "Player stats were not returned",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // ------------------------------------------------
            // STATS
            // ------------------------------------------------

            int mmr =
                    stats.optInt(
                            "mmr",
                            1000
                    );

            int wins =
                    stats.optInt(
                            "wins",
                            0
                    );

            int losses =
                    stats.optInt(
                            "losses",
                            0
                    );

            int currentStreak =
                    stats.optInt(
                            "current_streak",
                            0
                    );

            int bestStreak =
                    stats.optInt(
                            "best_streak",
                            0
                    );

            double winRate =
                    stats.optDouble(
                            "win_rate",
                            calculateWinRate(
                                    wins,
                                    losses
                            )
                    );

            int bestTime =
                    -1;

            if (
                    stats.has(
                            "best_time_seconds"
                    ) &&
                            !stats.isNull(
                                    "best_time_seconds"
                            )
            ) {

                bestTime =
                        stats.optInt(
                                "best_time_seconds",
                                -1
                        );
            }

            // ------------------------------------------------
            // CACHE
            // ------------------------------------------------

            SharedPreferences.Editor editor =
                    sessionPrefs.edit();

            editor.putInt(
                    "mmr",
                    mmr
            );

            editor.putInt(
                    "wins",
                    wins
            );

            editor.putInt(
                    "losses",
                    losses
            );

            editor.putInt(
                    "current_streak",
                    currentStreak
            );

            editor.putInt(
                    "best_streak",
                    bestStreak
            );

            editor.putFloat(
                    "win_rate",
                    (float) winRate
            );

            if (
                    bestTime >=
                            0
            ) {

                editor.putInt(
                        "best_time_seconds",
                        bestTime
                );

            } else {

                editor.remove(
                        "best_time_seconds"
                );
            }

            editor.apply();

            // ------------------------------------------------
            // UI
            // ------------------------------------------------

            updateStatsUI(
                    mmr,
                    wins,
                    losses,
                    winRate,
                    currentStreak,
                    bestStreak,
                    bestTime
            );

            Log.d(
                    TAG,
                    "MMR=" +
                            mmr +
                            " W=" +
                            wins +
                            " L=" +
                            losses +
                            " BestTime=" +
                            bestTime
            );

        } catch (
                Exception e
        ) {

            Log.e(
                    TAG,
                    "Stats parsing error",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to read competitive stats",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ------------------------------------------------
    // UPDATE UI
    // ------------------------------------------------

    private void updateStatsUI(
            int mmr,
            int wins,
            int losses,
            double winRate,
            int currentStreak,
            int bestStreak,
            int bestTime
    ) {

        txtMmr.setText(
                String.valueOf(
                        mmr
                )
        );

        txtWins.setText(
                String.valueOf(
                        wins
                )
        );

        txtLosses.setText(
                String.valueOf(
                        losses
                )
        );

        txtWinRate.setText(
                formatWinRate(
                        winRate
                )
        );

        txtCurrentStreak.setText(
                String.valueOf(
                        currentStreak
                )
        );

        txtBestStreak.setText(
                bestStreak +
                        (
                                bestStreak ==
                                        1
                                        ? " win"
                                        : " wins"
                        )
        );

        // ------------------------------------------------
        // BEST COMPLETION TIME
        // ------------------------------------------------

        if (
                bestTime <
                        0
        ) {

            txtBestTime.setText(
                    "No record yet"
            );

        } else {

            txtBestTime.setText(
                    formatTime(
                            bestTime
                    )
            );
        }

        // ------------------------------------------------
        // RANK
        // ------------------------------------------------

        txtRank.setText(
                getRankName(
                        mmr
                )
        );

        txtRankEmblem.setText(
                getRankEmblem(
                        mmr
                )
        );
    }

    // ------------------------------------------------
    // WIN RATE
    // ------------------------------------------------

    private double calculateWinRate(
            int wins,
            int losses
    ) {

        int total =
                wins +
                        losses;

        if (
                total ==
                        0
        ) {

            return 0;
        }

        return (
                wins *
                        100.0
        ) /
                total;
    }

    private String formatWinRate(
            double value
    ) {

        if (
                Math.abs(
                        value -
                                Math.rint(
                                        value
                                )
                ) <
                        0.0001
        ) {

            return String.format(
                    Locale.getDefault(),
                    "%.0f%%",
                    value
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.1f%%",
                value
        );
    }

    // ------------------------------------------------
    // TIME
    // ------------------------------------------------

    private String formatTime(
            int totalSeconds
    ) {

        int minutes =
                totalSeconds /
                        60;

        int seconds =
                totalSeconds %
                        60;

        return String.format(
                Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds
        );
    }

    // ------------------------------------------------
    // RANK
    // ------------------------------------------------

    private String getRankName(
            int mmr
    ) {

        if (
                mmr >=
                        2000
        ) {

            return "Grandmaster";
        }

        if (
                mmr >=
                        1500
        ) {

            return "Master";
        }

        if (
                mmr >=
                        1100
        ) {

            return "Expert";
        }

        if (
                mmr >=
                        700
        ) {

            return "Advanced";
        }

        if (
                mmr > 0
        ) {

            return "Rookie";
        }

        return "Unranked";
    }

    private String getRankEmblem(
            int mmr
    ) {

        if (
                mmr >=
                        2000
        ) {

            return "♛";
        }

        if (
                mmr >=
                        1500
        ) {

            return "◆";
        }

        if (
                mmr >=
                        1100
        ) {

            return "✦";
        }

        if (
                mmr >=
                        700
        ) {

            return "★";
        }

        if (
                mmr >
                        0
        ) {

            return "✧";
        }

        return "✦";
    }

    // ------------------------------------------------
    // SESSION
    // ------------------------------------------------

    private String getSessionToken() {

        String token =
                sessionPrefs.getString(
                        "session_token",
                        ""
                );

        if (
                token ==
                        null
        ) {

            return "";
        }

        return token.trim();
    }

    // ------------------------------------------------
    // INVALID SESSION
    // ------------------------------------------------

    private void handleInvalidSession(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        sessionPrefs
                .edit()
                .clear()
                .apply();

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    // ------------------------------------------------
    // NAVIGATION
    // ------------------------------------------------

    private void openActivity(
            Class<?> activityClass
    ) {

        Intent intent =
                new Intent(
                        this,
                        activityClass
                );

        startActivity(
                intent
        );
    }

    // ------------------------------------------------
    // CLEANUP
    // ------------------------------------------------

    @Override
    protected void onDestroy() {

        if (
                requestQueue !=
                        null
        ) {

            requestQueue.cancelAll(
                    request ->
                            true
            );
        }

        super.onDestroy();
    }
}