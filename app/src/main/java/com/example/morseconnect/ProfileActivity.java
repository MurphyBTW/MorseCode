package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtAvatar;
    private TextView txtDisplayName;
    private TextView txtUsername;
    private TextView txtUserId;
    private TextView txtJoinDate;

    private Button btnEditProfile;

    private SharedPreferences sessionPrefs;

    private RequestQueue requestQueue;

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private static final String TAG =
            "PROFILE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        // ------------------------------------------------
        // SESSION
        // ------------------------------------------------

        sessionPrefs =
                getSharedPreferences(
                        SESSION_PREFS,
                        MODE_PRIVATE
                );

        // ------------------------------------------------
        // VOLLEY
        // ------------------------------------------------

        requestQueue =
                Volley.newRequestQueue(this);

        // ------------------------------------------------
        // VIEWS
        // ------------------------------------------------

        txtAvatar =
                findViewById(R.id.txtAvatar);

        txtDisplayName =
                findViewById(R.id.txtDisplayName);

        txtUsername =
                findViewById(R.id.txtUsername);

        txtUserId =
                findViewById(R.id.txtUserId);

        txtJoinDate =
                findViewById(R.id.txtJoinDate);

        btnEditProfile =
                findViewById(R.id.btnEditProfile);

        // ------------------------------------------------
        // SHOW CACHED PROFILE FIRST
        // ------------------------------------------------

        loadCachedProfile();

        // ------------------------------------------------
        // LOAD CURRENT PROFILE FROM SERVER
        // ------------------------------------------------

        loadProfileFromServer();

        // ------------------------------------------------
        // EDIT PROFILE
        // ------------------------------------------------

        btnEditProfile.setOnClickListener(v -> {

            Toast.makeText(
                    ProfileActivity.this,
                    "Profile editing will be connected to your account next.",
                    Toast.LENGTH_SHORT
            ).show();

        });

        // ------------------------------------------------
        // BOTTOM NAVIGATION
        // ------------------------------------------------

        findViewById(R.id.btnHome)
                .setOnClickListener(v ->
                        openActivity(
                                MainActivity.class
                        )
                );

        findViewById(R.id.btnLeaderboard)
                .setOnClickListener(v ->
                        openActivityByName(
                                "LeaderboardActivity"
                        )
                );

        findViewById(R.id.btnSettingsTab)
                .setOnClickListener(v ->
                        openActivity(
                                SettingsActivity.class
                        )
                );
    }

    // ------------------------------------------------
    // LOAD CACHED PROFILE
    // ------------------------------------------------

    private void loadCachedProfile() {

        long userId =
                sessionPrefs.getLong(
                        "user_id",
                        -1
                );

        String firstName =
                sessionPrefs.getString(
                        "first_name",
                        ""
                );

        String lastName =
                sessionPrefs.getString(
                        "last_name",
                        ""
                );

        String username =
                sessionPrefs.getString(
                        "username",
                        ""
                );

        String createdAt =
                sessionPrefs.getString(
                        "created_at",
                        ""
                );

        updateProfileUI(
                userId,
                firstName,
                lastName,
                username,
                createdAt
        );
    }

    // ------------------------------------------------
    // LOAD PROFILE FROM SUPABASE
    // ------------------------------------------------

    private void loadProfileFromServer() {

        String sessionToken =
                sessionPrefs.getString(
                        "session_token",
                        ""
                );

        if (sessionToken == null ||
                sessionToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "No session token found"
            );

            handleInvalidSession(
                    "Please log in again."
            );

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "profile";

        Log.d(
                TAG,
                "Loading profile"
        );

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            Log.d(
                                    TAG,
                                    "Profile response received"
                            );

                            handleProfileResponse(
                                    response
                            );
                        },

                        error -> {

                            Log.e(
                                    TAG,
                                    "Profile request failed",
                                    error
                            );

                            if (error.networkResponse == null) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Unable to refresh profile. Showing saved account information.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            int statusCode =
                                    error.networkResponse.statusCode;

                            String responseBody =
                                    "";

                            if (error.networkResponse.data != null) {

                                responseBody =
                                        new String(
                                                error.networkResponse.data
                                        );
                            }

                            Log.e(
                                    TAG,
                                    "HTTP " +
                                            statusCode +
                                            ": " +
                                            responseBody
                            );

                            if (statusCode == 401) {

                                String message =
                                        "Your login session is no longer valid.";

                                try {

                                    JSONObject errorJson =
                                            new JSONObject(
                                                    responseBody
                                            );

                                    message =
                                            errorJson.optString(
                                                    "message",
                                                    message
                                            );

                                } catch (Exception ignored) {
                                }

                                handleInvalidSession(
                                        message
                                );

                            } else {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Unable to load profile",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                ) {

                    @Override
                    public Map<String, String> getHeaders() {

                        Map<String, String> headers =
                                new HashMap<>();

                        String sessionToken =
                                sessionPrefs.getString(
                                        "session_token",
                                        ""
                                );

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
                };

        requestQueue.add(
                request
        );
    }

    // ------------------------------------------------
    // HANDLE PROFILE RESPONSE
    // ------------------------------------------------

    private void handleProfileResponse(
            JSONObject response
    ) {

        try {

            boolean success =
                    response.optBoolean(
                            "success",
                            false
                    );

            if (!success) {

                String message =
                        response.optString(
                                "message",
                                "Unable to load profile"
                        );

                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            JSONObject user =
                    response.optJSONObject(
                            "user"
                    );

            if (user == null) {

                Toast.makeText(
                        this,
                        "Invalid profile response",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            long userId =
                    user.optLong(
                            "id",
                            -1
                    );

            String firstName =
                    user.optString(
                            "first_name",
                            ""
                    );

            String lastName =
                    user.optString(
                            "last_name",
                            ""
                    );

            String username =
                    user.optString(
                            "username",
                            ""
                    );

            String email =
                    user.optString(
                            "email",
                            ""
                    );

            String role =
                    user.optString(
                            "role",
                            "user"
                    );

            String createdAt =
                    user.optString(
                            "created_at",
                            ""
                    );

            // ------------------------------------------------
            // PLAYER STATS
            // ------------------------------------------------

            JSONObject stats =
                    response.optJSONObject(
                            "stats"
                    );

            int mmr = 1000;
            int wins = 0;
            int losses = 0;
            int matchesPlayed = 0;
            int currentStreak = 0;
            int bestStreak = 0;
            int bestWpm = 0;
            double winRate = 0;

            if (stats != null) {

                mmr =
                        stats.optInt(
                                "mmr",
                                1000
                        );

                wins =
                        stats.optInt(
                                "wins",
                                0
                        );

                losses =
                        stats.optInt(
                                "losses",
                                0
                        );

                matchesPlayed =
                        stats.optInt(
                                "matches_played",
                                0
                        );

                currentStreak =
                        stats.optInt(
                                "current_streak",
                                0
                        );

                bestStreak =
                        stats.optInt(
                                "best_streak",
                                0
                        );

                bestWpm =
                        stats.optInt(
                                "best_wpm",
                                0
                        );

                winRate =
                        stats.optDouble(
                                "win_rate",
                                0
                        );
            }

            // ------------------------------------------------
            // UPDATE LOCAL SESSION CACHE
            // ------------------------------------------------

            sessionPrefs
                    .edit()

                    .putLong(
                            "user_id",
                            userId
                    )

                    .putString(
                            "first_name",
                            firstName
                    )

                    .putString(
                            "last_name",
                            lastName
                    )

                    .putString(
                            "username",
                            username
                    )

                    .putString(
                            "email",
                            email
                    )

                    .putString(
                            "role",
                            role
                    )

                    .putString(
                            "created_at",
                            createdAt
                    )

                    .putInt(
                            "mmr",
                            mmr
                    )

                    .putInt(
                            "wins",
                            wins
                    )

                    .putInt(
                            "losses",
                            losses
                    )

                    .putInt(
                            "matches_played",
                            matchesPlayed
                    )

                    .putInt(
                            "current_streak",
                            currentStreak
                    )

                    .putInt(
                            "best_streak",
                            bestStreak
                    )

                    .putInt(
                            "best_wpm",
                            bestWpm
                    )

                    .putFloat(
                            "win_rate",
                            (float) winRate
                    )

                    .apply();

            // ------------------------------------------------
            // UPDATE SCREEN
            // ------------------------------------------------

            updateProfileUI(
                    userId,
                    firstName,
                    lastName,
                    username,
                    createdAt
            );

            Log.d(
                    TAG,
                    "Profile loaded successfully"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Profile response parsing error",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to read profile information",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ------------------------------------------------
    // UPDATE PROFILE UI
    // ------------------------------------------------

    private void updateProfileUI(
            long userId,
            String firstName,
            String lastName,
            String username,
            String createdAt
    ) {

        String displayName =
                buildDisplayName(
                        firstName,
                        lastName
                );

        txtDisplayName.setText(
                displayName
        );

        if (username == null ||
                username.trim().isEmpty()) {

            txtUsername.setText(
                    "@username"
            );

        } else {

            txtUsername.setText(
                    "@" + username
            );
        }

        if (userId > 0) {

            txtUserId.setText(
                    String.valueOf(
                            userId
                    )
            );

        } else {

            txtUserId.setText(
                    "Not available"
            );
        }

        txtJoinDate.setText(
                formatJoinDate(
                        createdAt
                )
        );

        // ------------------------------------------------
        // AVATAR INITIAL
        // ------------------------------------------------

        String initial = "?";

        if (firstName != null &&
                !firstName.trim().isEmpty()) {

            initial =
                    firstName
                            .trim()
                            .substring(
                                    0,
                                    1
                            )
                            .toUpperCase(
                                    Locale.getDefault()
                            );

        } else if (
                username != null &&
                        !username.trim().isEmpty()
        ) {

            initial =
                    username
                            .trim()
                            .substring(
                                    0,
                                    1
                            )
                            .toUpperCase(
                                    Locale.getDefault()
                            );
        }

        txtAvatar.setText(
                initial
        );
    }

    // ------------------------------------------------
    // BUILD FULL NAME
    // ------------------------------------------------

    private String buildDisplayName(
            String firstName,
            String lastName
    ) {

        String first =
                firstName == null
                        ? ""
                        : firstName.trim();

        String last =
                lastName == null
                        ? ""
                        : lastName.trim();

        String fullName =
                (first + " " + last)
                        .trim();

        if (fullName.isEmpty()) {

            return "MorseConnect User";
        }

        return fullName;
    }

    // ------------------------------------------------
    // FORMAT JOIN DATE
    // ------------------------------------------------

    private String formatJoinDate(
            String createdAt
    ) {

        if (createdAt == null ||
                createdAt.trim().isEmpty()) {

            return "Not available";
        }

        try {

            String dateString =
                    createdAt.trim();

            // Supabase may return more than
            // 3 fractional-second digits.
            //
            // Example:
            // 2026-09-30T01:30:20.123456+00:00
            //
            // Reduce the fractional part to
            // milliseconds for SimpleDateFormat.

            int dotIndex =
                    dateString.indexOf('.');

            if (dotIndex >= 0) {

                int timezoneIndex =
                        dateString.indexOf(
                                '+',
                                dotIndex
                        );

                if (timezoneIndex < 0) {

                    timezoneIndex =
                            dateString.indexOf(
                                    '-',
                                    dotIndex
                            );
                }

                if (timezoneIndex < 0 &&
                        dateString.endsWith("Z")) {

                    timezoneIndex =
                            dateString.length() - 1;
                }

                if (timezoneIndex > dotIndex) {

                    String fraction =
                            dateString.substring(
                                    dotIndex + 1,
                                    timezoneIndex
                            );

                    if (fraction.length() > 3) {

                        fraction =
                                fraction.substring(
                                        0,
                                        3
                                );

                        dateString =
                                dateString.substring(
                                        0,
                                        dotIndex + 1
                                ) +
                                        fraction +
                                        dateString.substring(
                                                timezoneIndex
                                        );
                    }
                }
            }

            // Convert trailing Z to +00:00
            if (dateString.endsWith("Z")) {

                dateString =
                        dateString.substring(
                                0,
                                dateString.length() - 1
                        ) +
                                "+00:00";
            }

            SimpleDateFormat inputFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                            Locale.US
                    );

            inputFormat.setTimeZone(
                    TimeZone.getTimeZone(
                            "UTC"
                    )
            );

            Date date =
                    inputFormat.parse(
                            dateString
                    );

            if (date == null) {

                return "Not available";
            }

            SimpleDateFormat outputFormat =
                    new SimpleDateFormat(
                            "MMMM d, yyyy",
                            Locale.getDefault()
                    );

            return outputFormat.format(
                    date
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to format created_at: " +
                            createdAt,
                    e
            );

            // Fallback so the user still sees something.
            if (createdAt.length() >= 10) {

                return createdAt.substring(
                        0,
                        10
                );
            }

            return createdAt;
        }
    }

    // ------------------------------------------------
    // INVALID / EXPIRED SESSION
    // ------------------------------------------------

    private void handleInvalidSession(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        // Clear login/session information.
        sessionPrefs
                .edit()
                .clear()
                .apply();

        Intent intent =
                new Intent(
                        ProfileActivity.this,
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
    // OPEN ACTIVITY
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
    // OPEN ACTIVITY BY NAME
    // ------------------------------------------------

    private void openActivityByName(
            String className
    ) {

        try {

            Class<?> activityClass =
                    Class.forName(
                            "com.example.morseconnect." +
                                    className
                    );

            openActivity(
                    activityClass
            );

        } catch (ClassNotFoundException e) {

            Toast.makeText(
                    this,
                    "Leaderboards screen is coming next",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}