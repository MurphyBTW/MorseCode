package com.example.morseconnect;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "PROFILE";

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private TextView txtAvatar;
    private TextView txtDisplayName;
    private TextView txtUsername;
    private TextView txtEmail;
    private TextView txtUserId;
    private TextView txtJoinDate;

    private Button btnEditProfile;

    private SharedPreferences sessionPrefs;
    private RequestQueue requestQueue;

    private boolean profileRequestRunning = false;
    private boolean updateRequestRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sessionPrefs =
                getSharedPreferences(
                        SESSION_PREFS,
                        MODE_PRIVATE
                );

        requestQueue =
                Volley.newRequestQueue(this);

        bindViews();
        setupNavigation();

        loadCachedProfile();
        loadProfileFromServer();

        btnEditProfile.setOnClickListener(
                v -> showEditProfileDialog()
        );
    }

    private void bindViews() {

        txtAvatar =
                findViewById(
                        R.id.txtAvatar
                );

        txtDisplayName =
                findViewById(
                        R.id.txtDisplayName
                );

        txtUsername =
                findViewById(
                        R.id.txtUsername
                );

        txtEmail =
                findViewById(
                        R.id.txtEmail
                );

        txtUserId =
                findViewById(
                        R.id.txtUserId
                );

        txtJoinDate =
                findViewById(
                        R.id.txtJoinDate
                );

        btnEditProfile =
                findViewById(
                        R.id.btnEditProfile
                );
    }

    private void setupNavigation() {

        findViewById(R.id.btnHome)
                .setOnClickListener(
                        v -> openActivity(
                                MainActivity.class
                        )
                );

        findViewById(R.id.btnProfile)
                .setOnClickListener(
                        v -> {
                            // Already on Profile.
                        }
                );

        findViewById(R.id.btnLeaderboard)
                .setOnClickListener(
                        v -> openActivityByName(
                                "LeaderboardActivity"
                        )
                );

        findViewById(R.id.btnSettingsTab)
                .setOnClickListener(
                        v -> openActivity(
                                SettingsActivity.class
                        )
                );
    }

    // =========================================================
    // CACHED PROFILE
    // =========================================================

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

        String email =
                sessionPrefs.getString(
                        "email",
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
                email,
                createdAt
        );
    }

    // =========================================================
    // LOAD PROFILE FROM SERVER
    // =========================================================

    private void loadProfileFromServer() {

        if (profileRequestRunning) {
            return;
        }

        String sessionToken =
                getSessionToken();

        if (sessionToken.isEmpty()) {

            Log.e(
                    TAG,
                    "No session token found"
            );

            handleInvalidSession(
                    "Please log in again."
            );

            return;
        }

        profileRequestRunning = true;

        String url =
                ApiConfig.BASE_URL +
                        "profile";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            profileRequestRunning =
                                    false;

                            handleProfileResponse(
                                    response
                            );
                        },

                        error -> {

                            profileRequestRunning =
                                    false;

                            Log.e(
                                    TAG,
                                    "Profile request failed",
                                    error
                            );

                            if (
                                    error.networkResponse ==
                                            null
                            ) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Unable to refresh profile. Showing saved account information.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            int statusCode =
                                    error
                                            .networkResponse
                                            .statusCode;

                            String message =
                                    getErrorMessage(
                                            error.networkResponse.data,
                                            "Unable to load profile"
                                    );

                            if (
                                    statusCode ==
                                            401
                            ) {

                                handleInvalidSession(
                                        message
                                );

                            } else {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        message,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
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

        requestQueue.add(request);
    }

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

                Toast.makeText(
                        this,
                        response.optString(
                                "message",
                                "Unable to load profile"
                        ),
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

            updateProfileUI(
                    userId,
                    firstName,
                    lastName,
                    username,
                    email,
                    createdAt
            );

            Log.d(
                    TAG,
                    "Profile loaded successfully"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Profile parsing error",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to read profile information",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // EDIT PROFILE DIALOG
    // =========================================================

    private void showEditProfileDialog() {

        if (updateRequestRunning) {
            return;
        }

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

        String email =
                sessionPrefs.getString(
                        "email",
                        ""
                );

        LinearLayout container =
                createDialogContainer();

        TextView description =
                createDialogDescription(
                        "Update your account information. A verification code will be sent to the email address below before any changes are saved."
                );

        EditText inputFirstName =
                createEditText(
                        "First name",
                        firstName,
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_FLAG_CAP_WORDS
                );

        EditText inputLastName =
                createEditText(
                        "Last name",
                        lastName,
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_FLAG_CAP_WORDS
                );

        EditText inputEmail =
                createEditText(
                        "Email address",
                        email,
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                );

        container.addView(
                description
        );

        container.addView(
                inputFirstName
        );

        container.addView(
                inputLastName
        );

        container.addView(
                inputEmail
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Edit Profile"
                        )
                        .setView(
                                container
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "SEND OTP",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored -> {

                    styleDialog(
                            dialog
                    );

                    dialog
                            .getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            )
                            .setOnClickListener(
                                    v -> {

                                        String newFirstName =
                                                inputFirstName
                                                        .getText()
                                                        .toString()
                                                        .trim();

                                        String newLastName =
                                                inputLastName
                                                        .getText()
                                                        .toString()
                                                        .trim();

                                        String newEmail =
                                                inputEmail
                                                        .getText()
                                                        .toString()
                                                        .trim()
                                                        .toLowerCase(
                                                                Locale.US
                                                        );

                                        if (
                                                newFirstName
                                                        .isEmpty()
                                        ) {

                                            inputFirstName.setError(
                                                    "First name is required"
                                            );

                                            return;
                                        }

                                        if (
                                                newLastName
                                                        .isEmpty()
                                        ) {

                                            inputLastName.setError(
                                                    "Last name is required"
                                            );

                                            return;
                                        }

                                        if (
                                                newFirstName.length() >
                                                        50
                                        ) {

                                            inputFirstName.setError(
                                                    "Maximum 50 characters"
                                            );

                                            return;
                                        }

                                        if (
                                                newLastName.length() >
                                                        50
                                        ) {

                                            inputLastName.setError(
                                                    "Maximum 50 characters"
                                            );

                                            return;
                                        }

                                        if (
                                                !android.util.Patterns
                                                        .EMAIL_ADDRESS
                                                        .matcher(
                                                                newEmail
                                                        )
                                                        .matches()
                                        ) {

                                            inputEmail.setError(
                                                    "Enter a valid email"
                                            );

                                            return;
                                        }

                                        requestProfileOtp(
                                                dialog,
                                                newFirstName,
                                                newLastName,
                                                newEmail
                                        );
                                    }
                            );
                }
        );

        dialog.show();
    }

    // =========================================================
    // REQUEST OTP
    // =========================================================

    private void requestProfileOtp(
            AlertDialog editDialog,
            String firstName,
            String lastName,
            String email
    ) {

        if (updateRequestRunning) {
            return;
        }

        String sessionToken =
                getSessionToken();

        if (sessionToken.isEmpty()) {

            handleInvalidSession(
                    "Please log in again."
            );

            return;
        }

        updateRequestRunning = true;

        Button positiveButton =
                editDialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                );

        positiveButton.setEnabled(false);
        positiveButton.setText(
                "SENDING..."
        );

        JSONObject body =
                new JSONObject();

        try {

            body.put(
                    "action",
                    "request_otp"
            );

            body.put(
                    "first_name",
                    firstName
            );

            body.put(
                    "last_name",
                    lastName
            );

            body.put(
                    "email",
                    email
            );

        } catch (JSONException e) {

            updateRequestRunning = false;

            positiveButton.setEnabled(true);
            positiveButton.setText(
                    "SEND OTP"
            );

            Toast.makeText(
                    this,
                    "Unable to prepare profile update.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "profile-update";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            updateRequestRunning =
                                    false;

                            positiveButton.setEnabled(
                                    true
                            );

                            positiveButton.setText(
                                    "SEND OTP"
                            );

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        response.optString(
                                                "message",
                                                "Unable to send verification code."
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            editDialog.dismiss();

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Verification code sent to " +
                                            email,
                                    Toast.LENGTH_LONG
                            ).show();

                            showOtpDialog(
                                    email
                            );
                        },

                        error -> {

                            updateRequestRunning =
                                    false;

                            positiveButton.setEnabled(
                                    true
                            );

                            positiveButton.setText(
                                    "SEND OTP"
                            );

                            handleUpdateError(
                                    error.networkResponse != null
                                            ? error.networkResponse.statusCode
                                            : -1,

                                    error.networkResponse != null
                                            ? error.networkResponse.data
                                            : null,

                                    "Unable to send verification code."
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

        requestQueue.add(request);
    }

    // =========================================================
    // OTP DIALOG
    // =========================================================

    private void showOtpDialog(
            String email
    ) {

        LinearLayout container =
                createDialogContainer();

        TextView description =
                createDialogDescription(
                        "Enter the 6-digit verification code sent to:\n\n" +
                                email +
                                "\n\nThe code expires in 10 minutes."
                );

        EditText inputOtp =
                createEditText(
                        "6-digit code",
                        "",
                        InputType.TYPE_CLASS_NUMBER
                );

        inputOtp.setGravity(
                Gravity.CENTER
        );

        inputOtp.setLetterSpacing(
                0.25f
        );

        container.addView(
                description
        );

        container.addView(
                inputOtp
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Verify Changes"
                        )
                        .setView(
                                container
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "VERIFY",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored -> {

                    styleDialog(
                            dialog
                    );

                    dialog
                            .getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            )
                            .setOnClickListener(
                                    v -> {

                                        String otp =
                                                inputOtp
                                                        .getText()
                                                        .toString()
                                                        .trim();

                                        if (
                                                !otp.matches(
                                                        "\\d{6}"
                                                )
                                        ) {

                                            inputOtp.setError(
                                                    "Enter the 6-digit code"
                                            );

                                            return;
                                        }

                                        verifyProfileOtp(
                                                dialog,
                                                otp
                                        );
                                    }
                            );
                }
        );

        dialog.show();
    }

    // =========================================================
    // VERIFY OTP
    // =========================================================

    private void verifyProfileOtp(
            AlertDialog otpDialog,
            String otp
    ) {

        if (updateRequestRunning) {
            return;
        }

        if (
                getSessionToken()
                        .isEmpty()
        ) {

            otpDialog.dismiss();

            handleInvalidSession(
                    "Please log in again."
            );

            return;
        }

        updateRequestRunning = true;

        Button positiveButton =
                otpDialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                );

        positiveButton.setEnabled(false);
        positiveButton.setText(
                "VERIFYING..."
        );

        JSONObject body =
                new JSONObject();

        try {

            body.put(
                    "action",
                    "verify_otp"
            );

            body.put(
                    "otp",
                    otp
            );

        } catch (JSONException e) {

            updateRequestRunning = false;

            positiveButton.setEnabled(true);
            positiveButton.setText(
                    "VERIFY"
            );

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "profile-update";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            updateRequestRunning =
                                    false;

                            positiveButton.setEnabled(
                                    true
                            );

                            positiveButton.setText(
                                    "VERIFY"
                            );

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            if (!success) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        response.optString(
                                                "message",
                                                "Incorrect verification code."
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            JSONObject user =
                                    response.optJSONObject(
                                            "user"
                                    );

                            if (user != null) {

                                saveUpdatedUserToCache(
                                        user
                                );
                            }

                            otpDialog.dismiss();

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Profile updated successfully.",
                                    Toast.LENGTH_LONG
                            ).show();

                            loadCachedProfile();

                            loadProfileFromServer();
                        },

                        error -> {

                            updateRequestRunning =
                                    false;

                            positiveButton.setEnabled(
                                    true
                            );

                            positiveButton.setText(
                                    "VERIFY"
                            );

                            handleUpdateError(
                                    error.networkResponse != null
                                            ? error.networkResponse.statusCode
                                            : -1,

                                    error.networkResponse != null
                                            ? error.networkResponse.data
                                            : null,

                                    "Unable to verify code."
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

        requestQueue.add(request);
    }

    private void saveUpdatedUserToCache(
            JSONObject user
    ) {

        SharedPreferences.Editor editor =
                sessionPrefs.edit();

        if (user.has("id")) {

            editor.putLong(
                    "user_id",
                    user.optLong(
                            "id",
                            sessionPrefs.getLong(
                                    "user_id",
                                    -1
                            )
                    )
            );
        }

        if (user.has("first_name")) {

            editor.putString(
                    "first_name",
                    user.optString(
                            "first_name",
                            ""
                    )
            );
        }

        if (user.has("last_name")) {

            editor.putString(
                    "last_name",
                    user.optString(
                            "last_name",
                            ""
                    )
            );
        }

        if (user.has("username")) {

            editor.putString(
                    "username",
                    user.optString(
                            "username",
                            ""
                    )
            );
        }

        if (user.has("email")) {

            editor.putString(
                    "email",
                    user.optString(
                            "email",
                            ""
                    )
            );
        }

        if (user.has("role")) {

            editor.putString(
                    "role",
                    user.optString(
                            "role",
                            "user"
                    )
            );
        }

        if (user.has("created_at")) {

            editor.putString(
                    "created_at",
                    user.optString(
                            "created_at",
                            ""
                    )
            );
        }

        editor.apply();
    }

    // =========================================================
    // PROFILE UI
    // =========================================================

    private void updateProfileUI(
            long userId,
            String firstName,
            String lastName,
            String username,
            String email,
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

        if (
                username == null ||
                        username.trim().isEmpty()
        ) {

            txtUsername.setText(
                    "@username"
            );

        } else {

            txtUsername.setText(
                    "@" +
                            username
            );
        }

        if (
                email == null ||
                        email.trim().isEmpty()
        ) {

            txtEmail.setText(
                    "Not available"
            );

        } else {

            txtEmail.setText(
                    email
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

        String initial = "?";

        if (
                firstName != null &&
                        !firstName
                                .trim()
                                .isEmpty()
        ) {

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
                        !username
                                .trim()
                                .isEmpty()
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

    // =========================================================
    // DIALOG UI HELPERS
    // =========================================================

    private LinearLayout createDialogContainer() {

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        int horizontal =
                dp(22);

        int vertical =
                dp(8);

        container.setPadding(
                horizontal,
                vertical,
                horizontal,
                dp(6)
        );

        return container;
    }

    private TextView createDialogDescription(
            String text
    ) {

        TextView textView =
                new TextView(this);

        textView.setText(
                text
        );

        textView.setTextColor(
                Color.parseColor(
                        "#91A0B5"
                )
        );

        textView.setTextSize(
                13
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                dp(8),
                0,
                dp(12)
        );

        textView.setLayoutParams(
                params
        );

        return textView;
    }

    private EditText createEditText(
            String hint,
            String value,
            int inputType
    ) {

        EditText editText =
                new EditText(this);

        editText.setHint(
                hint
        );

        editText.setText(
                value == null
                        ? ""
                        : value
        );

        editText.setInputType(
                inputType
        );

        editText.setTextColor(
                Color.parseColor(
                        "#FFF4DF"
                )
        );

        editText.setHintTextColor(
                Color.parseColor(
                        "#78869A"
                )
        );

        editText.setSingleLine(
                true
        );

        editText.setPadding(
                dp(14),
                0,
                dp(14),
                0
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.parseColor(
                        "#171927"
                )
        );

        background.setStroke(
                dp(1),
                Color.parseColor(
                        "#343748"
                )
        );

        background.setCornerRadius(
                dp(10)
        );

        editText.setBackground(
                background
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                );

        params.setMargins(
                0,
                dp(7),
                0,
                dp(7)
        );

        editText.setLayoutParams(
                params
        );

        return editText;
    }

    private void styleDialog(
            AlertDialog dialog
    ) {

        if (
                dialog.getWindow() !=
                        null
        ) {

            GradientDrawable background =
                    new GradientDrawable();

            background.setColor(
                    Color.parseColor(
                            "#1A1C2B"
                    )
            );

            background.setCornerRadius(
                    dp(18)
            );

            dialog
                    .getWindow()
                    .setBackgroundDrawable(
                            background
                    );
        }

        Button positive =
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                );

        Button negative =
                dialog.getButton(
                        AlertDialog.BUTTON_NEGATIVE
                );

        if (positive != null) {

            positive.setTextColor(
                    Color.parseColor(
                            "#FFC45C"
                    )
            );
        }

        if (negative != null) {

            negative.setTextColor(
                    Color.parseColor(
                            "#B7B7C8"
                    )
            );
        }
    }

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

    // =========================================================
    // NETWORK HELPERS
    // =========================================================

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
                        getSessionToken()
        );

        return headers;
    }

    private void handleUpdateError(
            int statusCode,
            byte[] responseData,
            String fallbackMessage
    ) {

        String message =
                getErrorMessage(
                        responseData,
                        fallbackMessage
                );

        Log.e(
                TAG,
                "Profile update HTTP " +
                        statusCode +
                        ": " +
                        message
        );

        if (
                statusCode ==
                        401
        ) {

            handleInvalidSession(
                    message
            );

            return;
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private String getErrorMessage(
            byte[] responseData,
            String fallback
    ) {

        if (
                responseData == null ||
                        responseData.length == 0
        ) {

            return fallback;
        }

        try {

            String body =
                    new String(
                            responseData,
                            StandardCharsets.UTF_8
                    );

            JSONObject json =
                    new JSONObject(
                            body
                    );

            return json.optString(
                    "message",
                    fallback
            );

        } catch (Exception e) {

            return fallback;
        }
    }

    // =========================================================
    // NAME
    // =========================================================

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

    // =========================================================
    // JOIN DATE
    // =========================================================

    private String formatJoinDate(
            String createdAt
    ) {

        if (
                createdAt == null ||
                        createdAt
                                .trim()
                                .isEmpty()
        ) {

            return "Not available";
        }

        try {

            String dateString =
                    createdAt.trim();

            int dotIndex =
                    dateString.indexOf('.');

            if (dotIndex >= 0) {

                int timezoneIndex =
                        dateString.indexOf(
                                '+',
                                dotIndex
                        );

                if (
                        timezoneIndex <
                                0
                ) {

                    timezoneIndex =
                            dateString.indexOf(
                                    '-',
                                    dotIndex
                            );
                }

                if (
                        timezoneIndex <
                                0 &&
                                dateString.endsWith(
                                        "Z"
                                )
                ) {

                    timezoneIndex =
                            dateString.length() -
                                    1;
                }

                if (
                        timezoneIndex >
                                dotIndex
                ) {

                    String fraction =
                            dateString.substring(
                                    dotIndex + 1,
                                    timezoneIndex
                            );

                    if (
                            fraction.length() >
                                    3
                    ) {

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

            if (
                    dateString.endsWith(
                            "Z"
                    )
            ) {

                dateString =
                        dateString.substring(
                                0,
                                dateString.length() -
                                        1
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

            if (
                    createdAt.length() >=
                            10
            ) {

                return createdAt.substring(
                        0,
                        10
                );
            }

            return createdAt;
        }
    }

    // =========================================================
    // INVALID SESSION
    // =========================================================

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
                        ProfileActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private void openActivity(
            Class<?> activityClass
    ) {

        Intent intent =
                new Intent(
                        this,
                        activityClass
                );

        startActivity(intent);
    }

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

        } catch (
                ClassNotFoundException e
        ) {

            Toast.makeText(
                    this,
                    "Leaderboards screen is coming next",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}