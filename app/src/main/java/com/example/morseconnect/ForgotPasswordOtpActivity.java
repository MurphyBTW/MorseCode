package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordOtpActivity extends AppCompatActivity {

    EditText edtOtp;
    Button btnVerify;
    Button btnResend;
    TextView txtEmail;
    TextView txtResendTimer;

    String email;

    CountDownTimer countDownTimer;

    private static final String SUPABASE_API_KEY =
            "sb_publishable_DYAEFbzuk9uVLyYYxD7mxA_SG4aE1iN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_forgot_password_otp);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        edtOtp = findViewById(R.id.edtOtp);
        btnVerify = findViewById(R.id.btnVerify);
        btnResend = findViewById(R.id.btnResend);
        txtEmail = findViewById(R.id.txtEmail);
        txtResendTimer = findViewById(R.id.txtResendTimer);

        email = getIntent().getStringExtra("email");

        if (email == null || email.isEmpty()) {
            Toast.makeText(
                    this,
                    "Email information is missing",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        txtEmail.setText("Code sent to " + email);

        btnVerify.setOnClickListener(v -> verifyOtp());

        btnResend.setOnClickListener(v -> resendOtp());

        startResendCountdown(180);
    }

    private void verifyOtp() {

        String otp =
                edtOtp.getText().toString().trim();

        if (otp.isEmpty()) {
            edtOtp.setError(
                    "Enter the verification code"
            );
            edtOtp.requestFocus();
            return;
        }

        if (!otp.matches("\\d{6}")) {
            edtOtp.setError(
                    "The verification code must be 6 digits"
            );
            edtOtp.requestFocus();
            return;
        }

        btnVerify.setEnabled(false);

        JSONObject body = new JSONObject();

        try {
            body.put("email", email);
            body.put("otp", otp);

        } catch (JSONException e) {

            btnVerify.setEnabled(true);

            Toast.makeText(
                    this,
                    "Something went wrong",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String url =
                ApiConfig.BASE_URL +
                        "password-reset-verify";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,

                        response -> {

                            btnVerify.setEnabled(true);

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            String message =
                                    response.optString(
                                            "message",
                                            "Verification failed"
                                    );

                            if (success) {

                                String resetToken =
                                        response.optString(
                                                "reset_token",
                                                ""
                                        );

                                if (resetToken.isEmpty()) {

                                    Toast.makeText(
                                            this,
                                            "The reset session could not be created. Please request a new code.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                Intent intent =
                                        new Intent(
                                                ForgotPasswordOtpActivity.this,
                                                ResetPasswordActivity.class
                                        );

                                intent.putExtra(
                                        "email",
                                        email
                                );

                                intent.putExtra(
                                        "reset_token",
                                        resetToken
                                );

                                startActivity(intent);

                                finish();

                            } else {

                                Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        error -> {

                            btnVerify.setEnabled(true);

                            String message =
                                    "Network error. Please check your connection.";

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                if (statusCode == 400) {

                                    message =
                                            "The verification code must be 6 digits.";

                                } else if (statusCode == 401) {

                                    message =
                                            "The verification code is incorrect. Your password has not been changed.";

                                } else if (statusCode == 404) {

                                    message =
                                            "The verification code has expired or is no longer available. Please request a new code.";

                                } else if (statusCode == 429) {

                                    message =
                                            "Please wait before requesting another verification code.";

                                } else if (statusCode == 500) {

                                    message =
                                            "Server error. Please try again.";
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

    private void resendOtp() {

        btnResend.setEnabled(false);

        JSONObject body = new JSONObject();

        try {
            body.put("email", email);
            body.put("resend", true);

        } catch (JSONException e) {

            btnResend.setEnabled(true);

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

                            boolean success =
                                    response.optBoolean(
                                            "success",
                                            false
                                    );

                            String message =
                                    response.optString(
                                            "message",
                                            "Could not resend the code"
                                    );

                            if (success) {

                                edtOtp.setText("");

                                Toast.makeText(
                                        this,
                                        "A new verification code has been sent to your email.",
                                        Toast.LENGTH_LONG
                                ).show();

                                startResendCountdown(180);

                            } else {

                                btnResend.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        error -> {

                            btnResend.setEnabled(true);

                            String message =
                                    "Network error. Please check your connection.";

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                if (statusCode == 429) {

                                    message =
                                            "Please wait before requesting another verification code.";

                                } else if (statusCode == 404) {

                                    message =
                                            "Reset request was not found. Please start again.";

                                } else if (statusCode == 500) {

                                    message =
                                            "Server error. Please try again.";
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

    private void startResendCountdown(int seconds) {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        btnResend.setEnabled(false);

        countDownTimer =
                new CountDownTimer(
                        seconds * 1000L,
                        1000
                ) {

                    @Override
                    public void onTick(long millisUntilFinished) {

                        long secondsRemaining =
                                (millisUntilFinished + 999) / 1000;

                        txtResendTimer.setText(
                                "Resend available in " +
                                        secondsRemaining +
                                        " seconds"
                        );
                    }

                    @Override
                    public void onFinish() {

                        txtResendTimer.setText(
                                "You can request a new verification code."
                        );

                        btnResend.setEnabled(true);
                    }
                };

        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {

        finish();

        return true;
    }
}