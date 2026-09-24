package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText edtEmail;
    Button btnReset;

    private boolean resetInProgress = false;

    private static final String SUPABASE_API_KEY =
            "sb_publishable_DYAEFbzuk9uVLyYYxD7mxA_SG4aE1iN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        edtEmail = findViewById(R.id.edtEmail);
        btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> sendResetCode());
    }

    private void sendResetCode() {

        if (resetInProgress) {
            return;
        }

        String email =
                edtEmail.getText().toString().trim();

        if (email.isEmpty()) {

            edtEmail.setError("Enter your email");
            edtEmail.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            edtEmail.setError("Enter a valid email");
            edtEmail.requestFocus();

            return;
        }

        resetInProgress = true;
        btnReset.setEnabled(false);

        JSONObject body = new JSONObject();

        try {

            body.put("email", email);

        } catch (JSONException e) {

            e.printStackTrace();

            resetInProgress = false;
            btnReset.setEnabled(true);

            Toast.makeText(
                    this,
                    "Something went wrong",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "password-reset";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            resetInProgress = false;
                            btnReset.setEnabled(true);

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            String message =
                                    response.optString(
                                            "message",
                                            ""
                                    );

                            if (success) {

                                Toast.makeText(
                                        this,
                                        "Verification code sent to your email",
                                        Toast.LENGTH_LONG
                                ).show();

                                Intent intent =
                                        new Intent(
                                                ForgotPasswordActivity.this,
                                                ForgotPasswordOtpActivity.class
                                        );

                                intent.putExtra(
                                        "email",
                                        email
                                );

                                startActivity(intent);

                            } else {

                                Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        error -> {

                            resetInProgress = false;
                            btnReset.setEnabled(true);

                            String message =
                                    "Network error: check server connection";

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                if (statusCode == 400) {

                                    message =
                                            "Invalid email address";

                                } else if (statusCode == 429) {

                                    message =
                                            "Please wait before requesting another verification code.";

                                } else if (statusCode == 500) {

                                    message =
                                            "Server error";

                                } else {

                                    message =
                                            "Password reset failed";
                                }
                            }

                            Toast.makeText(
                                    this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
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
                                SUPABASE_API_KEY
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

    @Override
    public boolean onSupportNavigateUp() {

        finish();

        return true;
    }
}