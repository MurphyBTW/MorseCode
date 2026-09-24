package com.example.morseconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText edtPassword;
    EditText edtConfirmPassword;
    Button btnResetPassword;

    String email;
    String resetToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        email = getIntent().getStringExtra("email");
        resetToken = getIntent().getStringExtra("reset_token");

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {

        String password = edtPassword.getText().toString().trim();
        String confirmPassword =
                edtConfirmPassword.getText().toString().trim();

        if (password.isEmpty()) {
            edtPassword.setError("Enter a new password");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            edtPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            edtConfirmPassword.setError("Confirm your password");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Passwords do not match");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (email == null || email.isEmpty() ||
                resetToken == null || resetToken.isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid reset session",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnResetPassword.setEnabled(false);

        JSONObject requestBody = new JSONObject();

        try {
            requestBody.put("email", email);
            requestBody.put("reset_token", resetToken);
            requestBody.put("password", password);
        } catch (JSONException e) {
            btnResetPassword.setEnabled(true);

            Toast.makeText(
                    this,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String url =
                ApiConfig.BASE_URL + "password-reset-update";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,

                response -> {

                    btnResetPassword.setEnabled(true);

                    boolean success =
                            response.optBoolean("success", false);

                    String message =
                            response.optString(
                                    "message",
                                    "Something went wrong"
                            );

                    if (success) {

                        Toast.makeText(
                                ResetPasswordActivity.this,
                                "Password changed successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                ResetPasswordActivity.this,
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
                                ResetPasswordActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                },

                error -> {

                    btnResetPassword.setEnabled(true);

                    String message = "Network error";

                    if (error.networkResponse != null) {

                        int statusCode =
                                error.networkResponse.statusCode;

                        if (statusCode == 400) {
                            message =
                                    "Password must be at least 6 characters";
                        } else if (statusCode == 401) {
                            message =
                                    "Invalid or expired reset session";
                        } else if (statusCode == 404) {
                            message =
                                    "Reset session not found";
                        } else if (statusCode == 500) {
                            message =
                                    "Server error. Please try again.";
                        }
                    }

                    Toast.makeText(
                            ResetPasswordActivity.this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

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
}
