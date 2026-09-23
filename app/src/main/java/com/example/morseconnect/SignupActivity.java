package com.example.morseconnect;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText edtUser, edtPass;
    Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);
        btnCreate = findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {

        String user = edtUser.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(
                    this,
                    "Enter username and password",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnCreate.setEnabled(false);

        JSONObject body = new JSONObject();

        try {
            body.put("username", user);
            body.put("password", pass);
        } catch (JSONException e) {
            e.printStackTrace();
            btnCreate.setEnabled(true);
            return;
        }

        // Supabase Edge Function
        String url = ApiConfig.BASE_URL + "signup";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,

                // SUCCESS
                response -> {

                    btnCreate.setEnabled(true);

                    try {

                        boolean success =
                                response.getBoolean("success");

                        String message =
                                response.optString(
                                        "message",
                                        ""
                                );

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        if (success) {
                            // Return to login screen
                            finish();
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

                // ERROR
                error -> {

                    btnCreate.setEnabled(true);

                    if (error.networkResponse != null) {

                        int statusCode =
                                error.networkResponse.statusCode;

                        String message;

                        if (statusCode == 409) {
                            message = "Username already exists";
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

                // Your Supabase publishable key
                headers.put(
                        "apikey",
                        "sb_publishable_DYAEFbzuk9uVLyYYxD7mxA_SG4aE1iN"
                );

                return headers;
            }
        };

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