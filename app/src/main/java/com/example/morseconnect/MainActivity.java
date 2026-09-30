package com.example.morseconnect;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnLibrary, btnQuiz;
    Button btnDailyChallenge, btnChallenge;

    View btnProfile, btnLeaderboard, btnSettingsTab;
    View signalLamp;
    TextView txtSignalCode;

    private final String[] signalStates = {
            "· · ·  — —  ·",
            "—  · — ·  · ·",
            "· —  · · ·  —",
            "— · ·  · —  · ·"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        String role = getIntent().getStringExtra("role");
        if ("admin".equals(role)) {
            Toast.makeText(this, "Admin Mode",
                    Toast.LENGTH_SHORT).show();
        }

        btnLibrary = findViewById(R.id.btnLibrary);
        btnQuiz = findViewById(R.id.btnQuiz);
        btnDailyChallenge = findViewById(R.id.btnDailyChallenge);
        btnChallenge = findViewById(R.id.btnChallenge);

        btnProfile = findViewById(R.id.btnProfile);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnSettingsTab = findViewById(R.id.btnSettingsTab);

        signalLamp = findViewById(R.id.signalLamp);
        txtSignalCode = findViewById(R.id.txtSignalCode);

        // Library Mode
        btnLibrary.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> startActivity(
                    new Intent(MainActivity.this,
                            LibraryModeActivity.class)), 90);
        });

        // Quiz Mode
        btnQuiz.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> startActivity(
                    new Intent(MainActivity.this,
                            QuizModeActivity.class)), 90);
        });

        // Daily Challenge
        btnDailyChallenge.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> startActivity(
                    new Intent(MainActivity.this,
                            DailyChallengeActivity.class)), 90);
        });

        // Challenge Mode
        btnChallenge.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> startActivity(
                    new Intent(MainActivity.this,
                            ChallengeModeActivity.class)), 90);
        });

        // Bottom navigation: Profile
        btnProfile.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> openPage("ProfileActivity"), 90);
        });

        // Bottom navigation: Leaderboards / Trophy Hub
        btnLeaderboard.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> openPage("LeaderboardActivity"), 90);
        });

        // Bottom navigation: Settings
        btnSettingsTab.setOnClickListener(v -> {
            pressAnimation(v);
            v.postDelayed(() -> startActivity(
                    new Intent(MainActivity.this,
                            SettingsActivity.class)), 90);
        });

        startSignalAnimation();
    }

    private void openPage(String activityName) {
        try {
            Class<?> page = Class.forName(
                    getPackageName() + "." + activityName);
            startActivity(new Intent(this, page));
        } catch (ClassNotFoundException e) {
            Toast.makeText(this,
                    activityName.replace("Activity", "")
                            + " screen is next to be created",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void pressAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                view, View.SCALE_X, 1f, 0.97f, 1f);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                view, View.SCALE_Y, 1f, 0.97f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(120);
        set.setInterpolator(
                new AccelerateDecelerateInterpolator());
        set.start();
    }

    private void startSignalAnimation() {
        ValueAnimator pulse = ValueAnimator.ofFloat(
                0.55f, 1f, 0.55f);

        pulse.setDuration(1200);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(
                new AccelerateDecelerateInterpolator());

        pulse.addUpdateListener(animation -> {
            float value =
                    (float) animation.getAnimatedValue();

            signalLamp.setAlpha(value);
            signalLamp.setScaleX(0.94f + (value * 0.06f));
            signalLamp.setScaleY(0.94f + (value * 0.06f));
        });

        pulse.start();

        ValueAnimator codeAnimator = ValueAnimator.ofInt(
                0, signalStates.length - 1);

        codeAnimator.setDuration(4800);
        codeAnimator.setRepeatCount(ValueAnimator.INFINITE);

        codeAnimator.addUpdateListener(animation -> {
            int index =
                    (int) animation.getAnimatedValue();

            txtSignalCode.setText(signalStates[index]);
        });

        codeAnimator.start();
    }
}