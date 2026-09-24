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

public class VerifyOtpActivity extends AppCompatActivity {

    EditText edtOtp;
    Button btnVerify;
    Button btnResend;
    TextView txtEmail;
    TextView txtResendTimer;

    private String username;
    private String email;

    private CountDownTimer countDownTimer;

    private static final String SUPABASE_API_KEY =
            "sb_publishable_DYAEFbzuk9uVLyYYxD7mxA_SG4aE1iN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_verify_otp);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        edtOtp = findViewById(R.id.edtOtp);
        btnVerify = findViewById(R.id.btnVerify);
        btnResend = findViewById(R.id.btnResend);
        txtEmail = findViewById(R.id.txtEmail);
        txtResendTimer = findViewById(R.id.txtResendTimer);

        username = getIntent().getStringExtra("username");
        email = getIntent().getStringExtra("email");

        if (email != null) {
            txtEmail.setText("Code sent to " + email);
        }

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
                    "OTP must be 6 digits"
            );

            edtOtp.requestFocus();

            return;
        }

        if (username == null || email == null) {

            Toast.makeText(
                    this,
                    "Signup information is missing",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnVerify.setEnabled(false);

        JSONObject body = new JSONObject();

        try {

            body.put("username", username);
            body.put("email", email);
            body.put("otp", otp);

        } catch (JSONException e) {

            e.printStackTrace();

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
                        "signup-verify";

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
                                            ""
                                    );

                            if (success) {

                                Toast.makeText(
                                        this,
                                        "Account created successfully",
                                        Toast.LENGTH_LONG
                                ).show();

                                Intent intent =
                                        new Intent(
                                                VerifyOtpActivity.this,
                                                LoginActivity.class
                                        );

                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                                    "Network error: check server connection";

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                if (statusCode == 400) {

                                    message =
                                            "Invalid verification code";

                                } else if (statusCode == 401) {

                                    message =
                                            "Invalid verification code";

                                } else if (statusCode == 404) {

                                    message =
                                            "Verification code expired or not found";

                                } else if (statusCode == 409) {

                                    message =
                                            "Username or email already exists";

                                } else if (statusCode == 500) {

                                    message =
                                            "Server error";

                                } else {

                                    message =
                                            "Verification failed";
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

        if (username == null || email == null) {

            Toast.makeText(
                    this,
                    "Signup information is missing",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnResend.setEnabled(false);

        JSONObject body = new JSONObject();

        try {

            body.put("username", username);
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
                        "signup";

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
                                    "Network error: check server connection";

                            if (error.networkResponse != null) {

                                int statusCode =
                                        error.networkResponse.statusCode;

                                if (statusCode == 429) {

                                    message =
                                            "Please wait before requesting another verification code.";

                                } else if (statusCode == 404) {

                                    message =
                                            "Signup request was not found. Please start again.";

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
                    public void onTick(
                            long millisUntilFinished
                    ) {

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