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

public class SignupActivity extends AppCompatActivity {

    EditText edtUser, edtEmail, edtPass, edtConfirmPass;
    Button btnCreate;

    // Prevents multiple signup requests from being started
    private boolean signupInProgress = false;

    private static final String SUPABASE_API_KEY =
            "sb_publishable_DYAEFbzuk9uVLyYYxD7mxA_SG4aE1iN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        edtUser = findViewById(R.id.edtUser);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);
        btnCreate = findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {

        // Prevent duplicate signup requests
        if (signupInProgress) {
            return;
        }

        String user = edtUser.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString();
        String confirmPass = edtConfirmPass.getText().toString();

        // Validate username
        if (user.isEmpty()) {
            edtUser.setError("Enter a username");
            edtUser.requestFocus();
            return;
        }

        // Validate email
        if (email.isEmpty()) {
            edtEmail.setError("Enter your email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Enter a valid email");
            edtEmail.requestFocus();
            return;
        }

        // Validate password
        if (pass.isEmpty()) {
            edtPass.setError("Enter a password");
            edtPass.requestFocus();
            return;
        }

        if (pass.length() < 6) {
            edtPass.setError("Password must be at least 6 characters");
            edtPass.requestFocus();
            return;
        }

        // Confirm password
        if (!pass.equals(confirmPass)) {
            edtConfirmPass.setError("Passwords do not match");
            edtConfirmPass.requestFocus();
            return;
        }

        // Signup request is now officially starting
        signupInProgress = true;
        btnCreate.setEnabled(false);

        JSONObject body = new JSONObject();

        try {
            body.put("username", user);
            body.put("email", email);
            body.put("password", pass);

        } catch (JSONException e) {

            e.printStackTrace();

            signupInProgress = false;
            btnCreate.setEnabled(true);

            Toast.makeText(
                    this,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String url = ApiConfig.BASE_URL + "signup";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,

                response -> {

                    // Request is finished
                    signupInProgress = false;
                    btnCreate.setEnabled(true);

                    try {

                        boolean success =
                                response.getBoolean("success");

                        String message =
                                response.optString(
                                        "message",
                                        ""
                                );

                        if (success) {

                            Toast.makeText(
                                    this,
                                    "Verification code sent to your email",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Intent intent =
                                    new Intent(
                                            SignupActivity.this,
                                            VerifyOtpActivity.class
                                    );

                            intent.putExtra("username", user);
                            intent.putExtra("email", email);

                            startActivity(intent);

                        } else {

                            Toast.makeText(
                                    this,
                                    message,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } catch (JSONException e) {

                        e.printStackTrace();

                        Toast.makeText(
                                this,
                                "Unexpected response",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                },

                error -> {

                    // Request is finished
                    signupInProgress = false;
                    btnCreate.setEnabled(true);

                    if (error.networkResponse != null) {

                        int statusCode =
                                error.networkResponse.statusCode;

                        String message;

                        if (statusCode == 409) {
                            message = "Username or email already exists";
                        } else if (statusCode == 400) {
                            message = "Invalid signup information";
                        } else if (statusCode == 500) {
                            message = "Server error";
                        } else {
                            message = "Signup failed";
                        }

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                "Network error: check server connection",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
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

        // IMPORTANT:
        // Prevent Volley from automatically retrying the signup POST.
        // This prevents duplicate OTP emails.
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