package com.example.morseconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    EditText edtUser, edtPass;
    Button btnLogin, btnSignup;
    TextView txtForgot;

    // ------------------------------------------------
    // SESSION STORAGE
    // ------------------------------------------------

    private static final String SESSION_PREFS =
            "morseconnect_session";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);
        txtForgot = findViewById(R.id.txtForgot);

        // ------------------------------------------------
        // FORGOT PASSWORD
        // ------------------------------------------------

        txtForgot.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    ForgotPasswordActivity.class
            );

            startActivity(intent);
        });

        // ------------------------------------------------
        // BACK BUTTON
        // ------------------------------------------------

        getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                }
        );

        // ------------------------------------------------
        // LOGIN
        // ------------------------------------------------

        btnLogin.setOnClickListener(
                v -> attemptLogin()
        );

        // ------------------------------------------------
        // SIGN UP
        // ------------------------------------------------

        btnSignup.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    SignupActivity.class
            );

            startActivity(intent);
        });
    }

    // ------------------------------------------------
    // LOGIN REQUEST
    // ------------------------------------------------

    private void attemptLogin() {

        String username =
                edtUser.getText()
                        .toString()
                        .trim();

        String password =
                edtPass.getText()
                        .toString();

        if (username.isEmpty() ||
                password.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter credentials",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnLogin.setEnabled(false);

        String url =
                ApiConfig.BASE_URL + "login";

        Log.d(
                "LOGIN_REQUEST",
                "URL: " + url
        );

        Log.d(
                "LOGIN_REQUEST",
                "Username: " + username
        );

        JSONObject jsonBody =
                new JSONObject();

        try {

            jsonBody.put(
                    "username",
                    username
            );

            jsonBody.put(
                    "password",
                    password
            );

        } catch (JSONException e) {

            btnLogin.setEnabled(true);

            Log.e(
                    "LOGIN_ERROR",
                    "Failed to create JSON",
                    e
            );

            return;
        }

        // ------------------------------------------------
        // VOLLEY REQUEST
        // ------------------------------------------------

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,

                        response -> {

                            btnLogin.setEnabled(true);

                            Log.d(
                                    "LOGIN_RESPONSE",
                                    "Server response received"
                            );

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
                                                    "Login failed"
                                            );

                                    int attemptsRemaining =
                                            response.optInt(
                                                    "attempts_remaining",
                                                    -1
                                            );

                                    if (attemptsRemaining >= 0) {

                                        message =
                                                message +
                                                        "\n" +
                                                        attemptsRemaining +
                                                        " attempt" +
                                                        (
                                                                attemptsRemaining == 1
                                                                        ? ""
                                                                        : "s"
                                                        ) +
                                                        " remaining.";
                                    }

                                    Toast.makeText(
                                            LoginActivity.this,
                                            message,
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                // ------------------------------------------------
                                // SESSION TOKEN
                                // ------------------------------------------------

                                String sessionToken =
                                        response.optString(
                                                "session_token",
                                                ""
                                        );

                                String sessionExpiresAt =
                                        response.optString(
                                                "session_expires_at",
                                                ""
                                        );

                                if (sessionToken.isEmpty()) {

                                    Log.e(
                                            "LOGIN_ERROR",
                                            "Server did not return session token"
                                    );

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Unable to create login session",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                // ------------------------------------------------
                                // USER OBJECT
                                // ------------------------------------------------

                                JSONObject userObject =
                                        response.optJSONObject(
                                                "user"
                                        );

                                if (userObject == null) {

                                    Log.e(
                                            "LOGIN_ERROR",
                                            "Server did not return user object"
                                    );

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Invalid account information",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                long userId =
                                        userObject.optLong(
                                                "id",
                                                -1
                                        );

                                String firstName =
                                        userObject.optString(
                                                "first_name",
                                                ""
                                        );

                                String lastName =
                                        userObject.optString(
                                                "last_name",
                                                ""
                                        );

                                String loggedInUsername =
                                        userObject.optString(
                                                "username",
                                                ""
                                        );

                                String email =
                                        userObject.optString(
                                                "email",
                                                ""
                                        );

                                String role =
                                        userObject.optString(
                                                "role",
                                                "user"
                                        );

                                String createdAt =
                                        userObject.optString(
                                                "created_at",
                                                ""
                                        );

                                if (userId <= 0) {

                                    Log.e(
                                            "LOGIN_ERROR",
                                            "Invalid user ID returned"
                                    );

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Invalid account information",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                // ------------------------------------------------
                                // SAVE LOGIN SESSION
                                // ------------------------------------------------

                                SharedPreferences sessionPrefs =
                                        getSharedPreferences(
                                                SESSION_PREFS,
                                                MODE_PRIVATE
                                        );

                                sessionPrefs
                                        .edit()

                                        .putString(
                                                "session_token",
                                                sessionToken
                                        )

                                        .putString(
                                                "session_expires_at",
                                                sessionExpiresAt
                                        )

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
                                                loggedInUsername
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

                                        .putBoolean(
                                                "logged_in",
                                                true
                                        )

                                        .apply();

                                Log.d(
                                        "LOGIN_SUCCESS",
                                        "Login successful for user ID: " +
                                                userId
                                );

                                // ------------------------------------------------
                                // OPEN MAIN ACTIVITY
                                // ------------------------------------------------

                                Intent intent =
                                        new Intent(
                                                LoginActivity.this,
                                                MainActivity.class
                                        );

                                // Keep these for compatibility with
                                // your existing MainActivity code.

                                intent.putExtra(
                                        "role",
                                        role
                                );

                                intent.putExtra(
                                        "user_id",
                                        String.valueOf(userId)
                                );

                                startActivity(intent);

                                finish();

                            } catch (Exception e) {

                                Log.e(
                                        "LOGIN_ERROR",
                                        "Invalid login response",
                                        e
                                );

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Invalid server response",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        error -> {

                            btnLogin.setEnabled(true);

                            Log.e(
                                    "LOGIN_ERROR",
                                    "Volley request failed",
                                    error
                            );

                            // ------------------------------------------------
                            // SERVER RETURNED HTTP RESPONSE
                            // ------------------------------------------------

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                String responseBody = "";

                                if (error.networkResponse.data != null) {

                                    responseBody =
                                            new String(
                                                    error.networkResponse.data
                                            );

                                    Log.e(
                                            "LOGIN_ERROR",
                                            "Server response: " +
                                                    responseBody
                                    );
                                }

                                Log.e(
                                        "LOGIN_ERROR",
                                        "HTTP Status: " +
                                                statusCode
                                );

                                try {

                                    JSONObject errorJson =
                                            new JSONObject(
                                                    responseBody
                                            );

                                    String message =
                                            errorJson.optString(
                                                    "message",
                                                    "Login failed"
                                            );

                                    // ------------------------------------------------
                                    // ACCOUNT LOCKED
                                    // ------------------------------------------------

                                    if (statusCode == 429) {

                                        int retryAfter =
                                                errorJson.optInt(
                                                        "retry_after",
                                                        180
                                                );

                                        String lockMessage =
                                                message +
                                                        "\nTry again in " +
                                                        retryAfter +
                                                        " seconds.";

                                        Toast.makeText(
                                                LoginActivity.this,
                                                lockMessage,
                                                Toast.LENGTH_LONG
                                        ).show();

                                    }

                                    // ------------------------------------------------
                                    // WRONG PASSWORD / USER
                                    // ------------------------------------------------

                                    else if (statusCode == 401) {

                                        int attemptsRemaining =
                                                errorJson.optInt(
                                                        "attempts_remaining",
                                                        -1
                                                );

                                        if (attemptsRemaining >= 0) {

                                            message =
                                                    message +
                                                            "\n" +
                                                            attemptsRemaining +
                                                            " attempt" +
                                                            (
                                                                    attemptsRemaining == 1
                                                                            ? ""
                                                                            : "s"
                                                            ) +
                                                            " remaining.";
                                        }

                                        Toast.makeText(
                                                LoginActivity.this,
                                                message,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    }

                                    // ------------------------------------------------
                                    // BAD REQUEST
                                    // ------------------------------------------------

                                    else if (statusCode == 400) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                message,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    }

                                    // ------------------------------------------------
                                    // SERVER ERROR
                                    // ------------------------------------------------

                                    else if (statusCode >= 500) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Server error. Please try again later.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                    }

                                    // ------------------------------------------------
                                    // OTHER ERROR
                                    // ------------------------------------------------

                                    else {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                message,
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }

                                } catch (JSONException e) {

                                    Log.e(
                                            "LOGIN_ERROR",
                                            "Could not parse server response",
                                            e
                                    );

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Login failed",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                            }

                            // ------------------------------------------------
                            // NETWORK ERROR
                            // ------------------------------------------------

                            else {

                                Log.e(
                                        "LOGIN_ERROR",
                                        "No HTTP response: " +
                                                error.toString(),
                                        error
                                );

                                if (error.getCause() != null) {

                                    Log.e(
                                            "LOGIN_ERROR",
                                            "CAUSE: " +
                                                    error.getCause()
                                                            .toString(),
                                            error.getCause()
                                    );
                                }

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Network error",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                ) {

                    // ------------------------------------------------
                    // REQUEST HEADERS
                    // ------------------------------------------------

                    @Override
                    public java.util.Map<String, String>
                    getHeaders() {

                        java.util.Map<String, String> headers =
                                new java.util.HashMap<>();

                        headers.put(
                                "Content-Type",
                                "application/json"
                        );

                        headers.put(
                                "apikey",
                                ApiConfig.SUPABASE_ANON_KEY
                        );

                        return headers;
                    }
                };

        // ------------------------------------------------
        // SEND REQUEST
        // ------------------------------------------------

        RequestQueue queue =
                Volley.newRequestQueue(this);

        queue.add(request);
    }
}