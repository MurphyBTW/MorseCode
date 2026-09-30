package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {

    private static final String TAG =
            "LEADERBOARD";

    private static final String SESSION_PREFS =
            "morseconnect_session";

    // ------------------------------------------------
    // PLAYER STATS
    // ------------------------------------------------

    private TextView txtRankEmblem;
    private TextView txtRank;
    private TextView txtMmr;
    private TextView txtWins;
    private TextView txtLosses;
    private TextView txtWinRate;
    private TextView txtCurrentStreak;
    private TextView txtBestStreak;
    private TextView txtBestTime;

    // ------------------------------------------------
    // LEADERBOARD
    // ------------------------------------------------

    private LinearLayout leaderboardHeader;
    private LinearLayout leaderboardContent;
    private LinearLayout leaderboardRows;

    private TextView txtLeaderboardArrow;
    private TextView txtLeaderboardStatus;

    // ------------------------------------------------
    // SESSION / NETWORK
    // ------------------------------------------------

    private SharedPreferences sessionPrefs;
    private RequestQueue requestQueue;

    private boolean leaderboardExpanded = false;
    private boolean leaderboardLoaded = false;
    private boolean initialProfileRequestSent = false;

    // ------------------------------------------------
    // CREATE
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
        // STAT VIEWS
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
        // LEADERBOARD VIEWS
        // ------------------------------------------------

        leaderboardHeader =
                findViewById(
                        R.id.leaderboardHeader
                );

        leaderboardContent =
                findViewById(
                        R.id.leaderboardContent
                );

        leaderboardRows =
                findViewById(
                        R.id.leaderboardRows
                );

        txtLeaderboardArrow =
                findViewById(
                        R.id.txtLeaderboardArrow
                );

        txtLeaderboardStatus =
                findViewById(
                        R.id.txtLeaderboardStatus
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
        // DROPDOWN
        // ------------------------------------------------

        leaderboardHeader.setOnClickListener(
                v -> toggleLeaderboard()
        );

        // ------------------------------------------------
        // CACHED STATS
        // ------------------------------------------------

        loadCachedStats();

        // ------------------------------------------------
        // SERVER PROFILE
        // ------------------------------------------------

        loadStatsFromServer();

        initialProfileRequestSent = true;
    }

    // ------------------------------------------------
    // RESUME
    // ------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        if (
                initialProfileRequestSent
        ) {
            loadStatsFromServer();

            if (
                    leaderboardExpanded
            ) {
                loadLeaderboard();
            }
        }
    }

    // ------------------------------------------------
    // TOGGLE LEADERBOARD
    // ------------------------------------------------

    private void toggleLeaderboard() {

        leaderboardExpanded =
                !leaderboardExpanded;

        if (
                leaderboardExpanded
        ) {

            leaderboardContent.setVisibility(
                    View.VISIBLE
            );

            txtLeaderboardArrow.setText(
                    "▲"
            );

            loadLeaderboard();

        } else {

            leaderboardContent.setVisibility(
                    View.GONE
            );

            txtLeaderboardArrow.setText(
                    "▼"
            );
        }
    }

    // ------------------------------------------------
    // CACHED ACCOUNT STATS
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
    // LOAD OWN PROFILE
    // ------------------------------------------------

    private void loadStatsFromServer() {

        String token =
                getSessionToken();

        if (
                token.isEmpty()
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

                        this::handleStatsResponse,

                        error -> {

                            Log.e(
                                    TAG,
                                    "Profile stats request failed",
                                    error
                            );

                            if (
                                    error.networkResponse !=
                                            null &&
                                            error.networkResponse.statusCode ==
                                                    401
                            ) {

                                handleInvalidSession(
                                        "Your login session is no longer valid."
                                );
                            }
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

        requestQueue.add(
                request
        );
    }

    // ------------------------------------------------
    // HANDLE OWN STATS
    // ------------------------------------------------

    private void handleStatsResponse(
            JSONObject response
    ) {

        try {

            if (
                    !response.optBoolean(
                            "success",
                            false
                    )
            ) {
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
                return;
            }

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

            updateStatsUI(
                    mmr,
                    wins,
                    losses,
                    winRate,
                    currentStreak,
                    bestStreak,
                    bestTime
            );

        } catch (
                Exception e
        ) {

            Log.e(
                    TAG,
                    "Unable to parse profile stats",
                    e
            );
        }
    }

    // ------------------------------------------------
    // LOAD TOP 10 LEADERBOARD
    // ------------------------------------------------

    private void loadLeaderboard() {

        String token =
                getSessionToken();

        if (
                token.isEmpty()
        ) {

            handleInvalidSession(
                    "Please log in again."
            );

            return;
        }

        txtLeaderboardStatus.setVisibility(
                View.VISIBLE
        );

        txtLeaderboardStatus.setText(
                leaderboardLoaded
                        ? "Refreshing leaderboard..."
                        : "Loading leaderboard..."
        );

        String url =
                ApiConfig.BASE_URL +
                        "leaderboard";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response ->
                                handleLeaderboardResponse(
                                        response
                                ),

                        error -> {

                            Log.e(
                                    TAG,
                                    "Leaderboard request failed",
                                    error
                            );

                            if (
                                    error.networkResponse !=
                                            null &&
                                            error.networkResponse.statusCode ==
                                                    401
                            ) {

                                handleInvalidSession(
                                        "Your login session is no longer valid."
                                );

                                return;
                            }

                            txtLeaderboardStatus.setVisibility(
                                    View.VISIBLE
                            );

                            txtLeaderboardStatus.setText(
                                    "Unable to load leaderboard"
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

        requestQueue.add(
                request
        );
    }

    // ------------------------------------------------
    // HANDLE TOP 10
    // ------------------------------------------------

    private void handleLeaderboardResponse(
            JSONObject response
    ) {

        try {

            boolean success =
                    response.optBoolean(
                            "success",
                            false
                    );

            if (
                    !success
            ) {

                txtLeaderboardStatus.setVisibility(
                        View.VISIBLE
                );

                txtLeaderboardStatus.setText(
                        response.optString(
                                "message",
                                "Unable to load leaderboard"
                        )
                );

                return;
            }

            JSONArray leaderboard =
                    response.optJSONArray(
                            "leaderboard"
                    );

            leaderboardRows.removeAllViews();

            if (
                    leaderboard ==
                            null ||
                            leaderboard.length() ==
                                    0
            ) {

                txtLeaderboardStatus.setVisibility(
                        View.VISIBLE
                );

                txtLeaderboardStatus.setText(
                        "No ranked players yet"
                );

                leaderboardLoaded =
                        true;

                return;
            }

            txtLeaderboardStatus.setVisibility(
                    View.GONE
            );

            long currentUserId =
                    sessionPrefs.getLong(
                            "user_id",
                            -1
                    );

            for (
                    int i = 0;
                    i < leaderboard.length();
                    i++
            ) {

                JSONObject player =
                        leaderboard.getJSONObject(
                                i
                        );

                int rank =
                        player.optInt(
                                "rank",
                                i + 1
                        );

                long userId =
                        player.optLong(
                                "user_id",
                                -1
                        );

                String username =
                        player.optString(
                                "username",
                                "Player"
                        );

                int mmr =
                        player.optInt(
                                "mmr",
                                0
                        );

                double winRate =
                        player.optDouble(
                                "win_rate",
                                0
                        );

                addLeaderboardRow(
                        rank,
                        username,
                        mmr,
                        winRate,
                        userId ==
                                currentUserId
                );
            }

            leaderboardLoaded =
                    true;

        } catch (
                Exception e
        ) {

            Log.e(
                    TAG,
                    "Leaderboard parsing error",
                    e
            );

            txtLeaderboardStatus.setVisibility(
                    View.VISIBLE
            );

            txtLeaderboardStatus.setText(
                    "Unable to read leaderboard"
            );
        }
    }

    // ------------------------------------------------
    // CREATE PLAYER ROW
    // ------------------------------------------------

    private void addLeaderboardRow(
            int rank,
            String username,
            int mmr,
            double winRate,
            boolean currentUser
    ) {

        LinearLayout row =
                new LinearLayout(
                        this
                );

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(14)
        );

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.setMargins(
                0,
                0,
                0,
                dp(2)
        );

        row.setLayoutParams(
                rowParams
        );

        if (
                currentUser
        ) {
            row.setBackgroundColor(
                    Color.rgb(
                            45,
                            42,
                            52
                    )
            );
        } else {
            row.setBackgroundColor(
                    Color.rgb(
                            26,
                            28,
                            43
                    )
            );
        }

        // ------------------------------------------------
        // RANK
        // ------------------------------------------------

        TextView rankView =
                new TextView(
                        this
                );

        rankView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(38),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        rankView.setText(
                String.valueOf(
                        rank
                )
        );

        rankView.setTextSize(
                14
        );

        rankView.setTypeface(
                null,
                Typeface.BOLD
        );

        if (
                rank <=
                        3
        ) {
            rankView.setTextColor(
                    Color.rgb(
                            255,
                            196,
                            92
                    )
            );
        } else {
            rankView.setTextColor(
                    Color.rgb(
                            183,
                            183,
                            200
                    )
            );
        }

        // ------------------------------------------------
        // USERNAME
        // ------------------------------------------------

        TextView usernameView =
                new TextView(
                        this
                );

        LinearLayout.LayoutParams usernameParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        usernameView.setLayoutParams(
                usernameParams
        );

        if (
                currentUser
        ) {
            usernameView.setText(
                    username +
                            "  • YOU"
            );
        } else {
            usernameView.setText(
                    username
            );
        }

        usernameView.setSingleLine(
                true
        );

        usernameView.setTextSize(
                14
        );

        usernameView.setTextColor(
                Color.rgb(
                        255,
                        244,
                        223
                )
        );

        if (
                currentUser
        ) {
            usernameView.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        // ------------------------------------------------
        // MMR
        // ------------------------------------------------

        TextView mmrView =
                new TextView(
                        this
                );

        mmrView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(72),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        mmrView.setGravity(
                Gravity.END
        );

        mmrView.setText(
                String.valueOf(
                        mmr
                )
        );

        mmrView.setTextSize(
                14
        );

        mmrView.setTypeface(
                null,
                Typeface.BOLD
        );

        mmrView.setTextColor(
                Color.rgb(
                        255,
                        196,
                        92
                )
        );

        // ------------------------------------------------
        // WIN RATE
        // ------------------------------------------------

        TextView wrView =
                new TextView(
                        this
                );

        wrView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(70),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        wrView.setGravity(
                Gravity.END
        );

        wrView.setText(
                formatWinRate(
                        winRate
                )
        );

        wrView.setTextSize(
                13
        );

        wrView.setTextColor(
                Color.rgb(
                        145,
                        160,
                        181
                )
        );

        // ------------------------------------------------
        // ADD
        // ------------------------------------------------

        row.addView(
                rankView
        );

        row.addView(
                usernameView
        );

        row.addView(
                mmrView
        );

        row.addView(
                wrView
        );

        leaderboardRows.addView(
                row
        );
    }

    // ------------------------------------------------
    // OWN STATS UI
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
    // HEADERS
    // ------------------------------------------------

    private Map<String, String> buildHeaders() {

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

    // ------------------------------------------------
    // TOKEN
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
            double winRate
    ) {

        if (
                Math.abs(
                        winRate -
                                Math.rint(
                                        winRate
                                )
                ) <
                        0.0001
        ) {

            return String.format(
                    Locale.getDefault(),
                    "%.0f%%",
                    winRate
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.1f%%",
                winRate
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
    // RANK NAME
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
                mmr >
                        0
        ) {
            return "Rookie";
        }

        return "Unranked";
    }

    // ------------------------------------------------
    // RANK EMBLEM
    // ------------------------------------------------

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
    // DP
    // ------------------------------------------------

    private int dp(
            int value
    ) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
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
    // DESTROY
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