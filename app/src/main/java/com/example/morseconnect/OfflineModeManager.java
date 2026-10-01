package com.example.morseconnect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public final class OfflineModeManager {

    private static final String SESSION_PREFS =
            "morseconnect_session";

    private static final String KEY_OFFLINE =
            "offline_mode";

    private OfflineModeManager() {
        // Utility class.
    }

    // ------------------------------------------------
    // CHECK MODE
    // ------------------------------------------------

    public static boolean isOffline(
            Context context
    ) {

        SharedPreferences prefs =
                context.getSharedPreferences(
                        SESSION_PREFS,
                        Context.MODE_PRIVATE
                );

        return prefs.getBoolean(
                KEY_OFFLINE,
                false
        );
    }

    // ------------------------------------------------
    // PROTECT ONLINE ACTIVITY
    // ------------------------------------------------

    public static boolean protect(
            AppCompatActivity activity,
            String featureName
    ) {

        if (
                !isOffline(
                        activity
                )
        ) {
            return false;
        }

        new AlertDialog.Builder(
                activity
        )
                .setTitle(
                        "Online Feature"
                )
                .setMessage(
                        featureName +
                                " requires a MorseConnect account.\n\n" +
                                "Sign in to access this feature."
                )
                .setCancelable(
                        false
                )
                .setNegativeButton(
                        "Go Back",
                        (dialog, which) ->
                                activity.finish()
                )
                .setPositiveButton(
                        "Sign In",
                        (dialog, which) -> {

                            SharedPreferences prefs =
                                    activity
                                            .getSharedPreferences(
                                                    SESSION_PREFS,
                                                    Context.MODE_PRIVATE
                                            );

                            prefs
                                    .edit()
                                    .putBoolean(
                                            KEY_OFFLINE,
                                            false
                                    )
                                    .apply();

                            Intent intent =
                                    new Intent(
                                            activity,
                                            LoginActivity.class
                                    );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            activity.startActivity(
                                    intent
                            );

                            activity.finish();
                        }
                )
                .show();

        return true;
    }
}