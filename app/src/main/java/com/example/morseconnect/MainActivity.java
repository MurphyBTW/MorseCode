package com.example.morseconnect;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SESSION_PREFS =
            "morseconnect_session";

    Button btnLibrary, btnQuiz;
    Button btnDailyChallenge, btnChallenge;

    View btnProfile;
    View btnLeaderboard;
    View btnSettingsTab;

    View signalLamp;
    TextView txtSignalCode;

    private SharedPreferences sessionPrefs;

    private boolean offlineMode = false;

    private final String[] signalStates = {
            "· · ·  — —  ·",
            "—  · — ·  · ·",
            "· —  · · ·  —",
            "— · ·  · —  · ·"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_main
        );

        // ------------------------------------------------
        // SESSION
        // ------------------------------------------------

        sessionPrefs =
                getSharedPreferences(
                        SESSION_PREFS,
                        MODE_PRIVATE
                );

        offlineMode =
                sessionPrefs.getBoolean(
                        "offline_mode",
                        false
                );

        // ------------------------------------------------
        // BACK BUTTON
        // ------------------------------------------------

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {
                                finish();
                            }
                        }
                );

        if (getSupportActionBar() != null) {

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(
                            false
                    );
        }

        // ------------------------------------------------
        // ADMIN ROLE
        // ------------------------------------------------

        String role =
                getIntent()
                        .getStringExtra(
                                "role"
                        );

        if (
                "admin".equals(role) &&
                        !offlineMode
        ) {

            Toast.makeText(
                    this,
                    "Admin Mode",
                    Toast.LENGTH_SHORT
            ).show();
        }

        // ------------------------------------------------
        // VIEWS
        // ------------------------------------------------

        btnLibrary =
                findViewById(
                        R.id.btnLibrary
                );

        btnQuiz =
                findViewById(
                        R.id.btnQuiz
                );

        btnDailyChallenge =
                findViewById(
                        R.id.btnDailyChallenge
                );

        btnChallenge =
                findViewById(
                        R.id.btnChallenge
                );

        btnProfile =
                findViewById(
                        R.id.btnProfile
                );

        btnLeaderboard =
                findViewById(
                        R.id.btnLeaderboard
                );

        btnSettingsTab =
                findViewById(
                        R.id.btnSettingsTab
                );

        signalLamp =
                findViewById(
                        R.id.signalLamp
                );

        txtSignalCode =
                findViewById(
                        R.id.txtSignalCode
                );

        // ------------------------------------------------
        // LIBRARY
        //
        // AVAILABLE ONLINE + OFFLINE
        // ------------------------------------------------

        btnLibrary.setOnClickListener(v -> {

            pressAnimation(v);

            v.postDelayed(
                    () -> startActivity(
                            new Intent(
                                    MainActivity.this,
                                    LibraryModeActivity.class
                            )
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // QUIZ
        //
        // ONLINE ONLY
        // ------------------------------------------------

        btnQuiz.setOnClickListener(v -> {

            pressAnimation(v);

            if (offlineMode) {

                showOnlineRequiredDialog(
                        "Quiz Mode"
                );

                return;
            }

            v.postDelayed(
                    () -> startActivity(
                            new Intent(
                                    MainActivity.this,
                                    QuizModeActivity.class
                            )
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // DAILY CHALLENGE
        //
        // ONLINE ONLY
        // ------------------------------------------------

        btnDailyChallenge.setOnClickListener(v -> {

            pressAnimation(v);

            if (offlineMode) {

                showOnlineRequiredDialog(
                        "Daily Challenge"
                );

                return;
            }

            v.postDelayed(
                    () -> startActivity(
                            new Intent(
                                    MainActivity.this,
                                    DailyChallengeActivity.class
                            )
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // CHALLENGE MODE
        //
        // ONLINE ONLY
        // ------------------------------------------------

        btnChallenge.setOnClickListener(v -> {

            pressAnimation(v);

            if (offlineMode) {

                showOnlineRequiredDialog(
                        "Challenge Mode"
                );

                return;
            }

            v.postDelayed(
                    () -> startActivity(
                            new Intent(
                                    MainActivity.this,
                                    ChallengeModeActivity.class
                            )
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // PROFILE
        //
        // ONLINE ONLY
        // ------------------------------------------------

        btnProfile.setOnClickListener(v -> {

            pressAnimation(v);

            if (offlineMode) {

                showOnlineRequiredDialog(
                        "Profile"
                );

                return;
            }

            v.postDelayed(
                    () -> openPage(
                            "ProfileActivity"
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // LEADERBOARD
        //
        // ONLINE ONLY
        // ------------------------------------------------

        btnLeaderboard.setOnClickListener(v -> {

            pressAnimation(v);

            if (offlineMode) {

                showOnlineRequiredDialog(
                        "Leaderboards"
                );

                return;
            }

            v.postDelayed(
                    () -> openPage(
                            "LeaderboardActivity"
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // SETTINGS
        //
        // AVAILABLE ONLINE + OFFLINE
        // ------------------------------------------------

        btnSettingsTab.setOnClickListener(v -> {

            pressAnimation(v);

            v.postDelayed(
                    () -> startActivity(
                            new Intent(
                                    MainActivity.this,
                                    SettingsActivity.class
                            )
                    ),
                    90
            );
        });

        // ------------------------------------------------
        // OFFLINE APPEARANCE
        // ------------------------------------------------

        updateOfflineAppearance();

        // ------------------------------------------------
        // ANIMATION
        // ------------------------------------------------

        startSignalAnimation();
    }

    // ------------------------------------------------
    // OFFLINE APPEARANCE
    // ------------------------------------------------

    private void updateOfflineAppearance() {

        if (!offlineMode) {
            return;
        }

        /*
         * We don't disable the buttons completely because
         * the user should still be able to tap them and see
         * WHY they are unavailable.
         */

        btnQuiz.setAlpha(
                0.45f
        );

        btnDailyChallenge.setAlpha(
                0.45f
        );

        btnChallenge.setAlpha(
                0.45f
        );

        btnProfile.setAlpha(
                0.45f
        );

        btnLeaderboard.setAlpha(
                0.45f
        );

        // Library remains fully available.

        btnLibrary.setAlpha(
                1f
        );

        // Settings also remains available.

        btnSettingsTab.setAlpha(
                1f
        );
    }

    // ------------------------------------------------
    // ONLINE REQUIRED DIALOG
    // ------------------------------------------------

    private void showOnlineRequiredDialog(
            String featureName
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Online Feature"
                )
                .setMessage(
                        featureName +
                                " requires a MorseConnect account.\n\n" +
                                "Sign in to access this feature."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Sign In",
                        (dialog, which) ->
                                exitOfflineAndOpenLogin()
                )
                .show();
    }

    // ------------------------------------------------
    // EXIT OFFLINE MODE
    // ------------------------------------------------

    private void exitOfflineAndOpenLogin() {

        sessionPrefs
                .edit()
                .putBoolean(
                        "offline_mode",
                        false
                )
                .apply();

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    // ------------------------------------------------
    // OPEN PAGE
    // ------------------------------------------------

    private void openPage(
            String activityName
    ) {

        try {

            Class<?> page =
                    Class.forName(
                            getPackageName() +
                                    "." +
                                    activityName
                    );

            startActivity(
                    new Intent(
                            this,
                            page
                    )
            );

        } catch (
                ClassNotFoundException e
        ) {

            Toast.makeText(
                    this,
                    activityName
                            .replace(
                                    "Activity",
                                    ""
                            ) +
                            " screen is next to be created",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ------------------------------------------------
    // PRESS ANIMATION
    // ------------------------------------------------

    private void pressAnimation(
            View view
    ) {

        ObjectAnimator scaleX =
                ObjectAnimator.ofFloat(
                        view,
                        View.SCALE_X,
                        1f,
                        0.97f,
                        1f
                );

        ObjectAnimator scaleY =
                ObjectAnimator.ofFloat(
                        view,
                        View.SCALE_Y,
                        1f,
                        0.97f,
                        1f
                );

        AnimatorSet set =
                new AnimatorSet();

        set.playTogether(
                scaleX,
                scaleY
        );

        set.setDuration(
                120
        );

        set.setInterpolator(
                new AccelerateDecelerateInterpolator()
        );

        set.start();
    }

    // ------------------------------------------------
    // SIGNAL ANIMATION
    // ------------------------------------------------

    private void startSignalAnimation() {

        ValueAnimator pulse =
                ValueAnimator.ofFloat(
                        0.55f,
                        1f,
                        0.55f
                );

        pulse.setDuration(
                1200
        );

        pulse.setRepeatCount(
                ValueAnimator.INFINITE
        );

        pulse.setInterpolator(
                new AccelerateDecelerateInterpolator()
        );

        pulse.addUpdateListener(
                animation -> {

                    float value =
                            (float)
                                    animation
                                            .getAnimatedValue();

                    signalLamp.setAlpha(
                            value
                    );

                    signalLamp.setScaleX(
                            0.94f +
                                    (
                                            value *
                                                    0.06f
                                    )
                    );

                    signalLamp.setScaleY(
                            0.94f +
                                    (
                                            value *
                                                    0.06f
                                    )
                    );
                }
        );

        pulse.start();

        ValueAnimator codeAnimator =
                ValueAnimator.ofInt(
                        0,
                        signalStates.length -
                                1
                );

        codeAnimator.setDuration(
                4800
        );

        codeAnimator.setRepeatCount(
                ValueAnimator.INFINITE
        );

        codeAnimator.addUpdateListener(
                animation -> {

                    int index =
                            (int)
                                    animation
                                            .getAnimatedValue();

                    txtSignalCode.setText(
                            signalStates[index]
                    );
                }
        );

        codeAnimator.start();
    }
}