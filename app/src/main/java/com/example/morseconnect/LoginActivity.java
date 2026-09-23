package com.example.morseconnect;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);
        txtForgot = findViewById(R.id.txtForgot);

        // Forgot password
        txtForgot.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    ForgotPasswordActivity.class
            );
            startActivity(intent);
        });

        // Back button
        getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                }
        );

        // Login
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Signup
        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    SignupActivity.class
            );
            startActivity(intent);
        });
    }

    private void attemptLogin() {

        String user = edtUser.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        // Check empty fields
        if (user.isEmpty() || pass.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter credentials",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Disable login button while request is running
        btnLogin.setEnabled(false);

        String url =
                ApiConfig.BASE_URL + "login";

        Log.d(
                "LOGIN_REQUEST",
                "URL: " + url
        );

        Log.d(
                "LOGIN_REQUEST",
                "Username: " + user
        );

        JSONObject jsonBody =
                new JSONObject();

        try {

            jsonBody.put(
                    "username",
                    user
            );

            jsonBody.put(
                    "password",
                    pass
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

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,

                        // ==========================================
                        // SUCCESS RESPONSE
                        // ==========================================

                        response -> {

                            btnLogin.setEnabled(true);

                            Log.d(
                                    "LOGIN_RESPONSE",
                                    "Server response: " +
                                            response.toString()
                            );

                            try {

                                boolean success =
                                        response.getBoolean(
                                                "success"
                                        );

                                // ----------------------------------
                                // LOGIN SUCCESSFUL
                                // ----------------------------------

                                if (success) {

                                    String role =
                                            response.optString(
                                                    "role",
                                                    ""
                                            );

                                    String userId =
                                            response.optString(
                                                    "user_id",
                                                    ""
                                            );

                                    Log.d(
                                            "LOGIN_SUCCESS",
                                            "Login successful"
                                    );

                                    Intent intent =
                                            new Intent(
                                                    LoginActivity.this,
                                                    MainActivity.class
                                            );

                                    intent.putExtra(
                                            "role",
                                            role
                                    );

                                    intent.putExtra(
                                            "user_id",
                                            userId
                                    );

                                    startActivity(intent);

                                    finish();

                                }

                                // ----------------------------------
                                // LOGIN FAILED
                                // ----------------------------------

                                else {

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
                                }

                            } catch (JSONException e) {

                                Log.e(
                                        "LOGIN_ERROR",
                                        "Invalid JSON response",
                                        e
                                );

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Invalid server response",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        // ==========================================
                        // ERROR RESPONSE
                        // ==========================================

                        error -> {

                            btnLogin.setEnabled(true);

                            Log.e(
                                    "LOGIN_ERROR",
                                    "Volley request failed",
                                    error
                            );

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                String responseBody =
                                        "";

                                if (
                                        error.networkResponse.data
                                                != null
                                ) {

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

                                    // ==================================
                                    // ACCOUNT LOCKED
                                    // ==================================

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

                                    // ==================================
                                    // WRONG PASSWORD / USER NOT FOUND
                                    // ==================================

                                    else if (statusCode == 401) {

                                        int attemptsRemaining =
                                                errorJson.optInt(
                                                        "attempts_remaining",
                                                        -1
                                                );

                                        if (
                                                attemptsRemaining >= 0
                                        ) {

                                            message =
                                                    message +
                                                            "\n" +
                                                            attemptsRemaining +
                                                            " attempt" +
                                                            (
                                                                    attemptsRemaining
                                                                            == 1
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

                                    // ==================================
                                    // BAD REQUEST
                                    // ==================================

                                    else if (statusCode == 400) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                message,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    }

                                    // ==================================
                                    // SERVER ERROR
                                    // ==================================

                                    else if (statusCode >= 500) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Server error. Please try again later.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                    }

                                    // ==================================
                                    // OTHER ERROR
                                    // ==================================

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

                            // ==========================================
                            // NO SERVER RESPONSE
                            // ==========================================

                            else {

                                Log.e(
                                        "LOGIN_ERROR",
                                        "No HTTP response: " +
                                                error.toString(),
                                        error
                                );

                                if (
                                        error.getCause() != null
                                ) {

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

                    // ==============================================
                    // SUPABASE HEADERS
                    // ==============================================

                    @Override
                    public java.util.Map<String, String>
                    getHeaders() {

                        java.util.Map<String, String>
                                headers =
                                new java.util.HashMap<>();

                        headers.put(
                                "Content-Type",
                                "application/json"
                        );

                        headers.put(
                                "apikey",
                                "sb_publishable_DYAEFbzuk9uVLyYYxD7mxA_SG4aE1iN"
                        );

                        return headers;
                    }
                };

        // ==============================================
        // SEND REQUEST
        // ==============================================

        RequestQueue queue =
                Volley.newRequestQueue(this);

        queue.add(request);
    }
}