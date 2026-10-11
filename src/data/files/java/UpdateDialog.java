package com.shadow.injector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * First launch dialog: forces (well, strongly encourages) the user to join the
 * Telegram channel before using the app.
 *
 * Buttons: JOIN CHANNEL (opens Telegram) / CLOSE (continue) / EXIT (close app)
 * plus a "Do not show again" checkbox that is remembered in SharedPreferences.
 *
 * Usage from Sketchware blocks:
 * <pre>
 *     UpdateDialog.showIfNeeded(MainActivity.this);   // onCreate
 *     UpdateDialog.openTelegram(MainActivity.this);   // any button
 * </pre>
 */
public class UpdateDialog {

    /** Telegram channel invite - change this one line to update the link. */
    public static final String TELEGRAM_URL = "https://t.me/+BBimnHMiSvpiYTBl";

    private static final String PREF_NAME = "shadow_prefs";
    private static final String KEY_SKIP = "skip_update_dialog";

    private UpdateDialog() {
    }

    /** Shows the dialog only the first time (unless "do not show again" was ticked). */
    public static void showIfNeeded(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (prefs.getBoolean(KEY_SKIP, false)) {
                return;
            }
        } catch (Throwable ignored) {
        }
        show(activity);
    }

    /** Shows the dialog every time it is called (ignores the preference). */
    public static void show(final Activity activity) {
        if (activity == null) {
            return;
        }
        final float d = activity.getResources().getDisplayMetrics().density;
        final int pad = (int) (22 * d);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.card_bg);
        root.setPadding(pad, pad, pad, (int) (14 * d));

        // ---- title ------------------------------------------------------
        TextView title = new TextView(activity);
        title.setText("JOIN OUR CHANNEL");
        title.setTextColor(0xFFEDE9FE);
        title.setTextSize(19f);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        // ---- message ----------------------------------------------------
        TextView message = new TextView(activity);
        message.setText("You must join our Telegram channel to receive future updates, new injections and bug fixes.");
        message.setTextColor(0xFF8B80B8);
        message.setTextSize(13f);
        message.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = (int) (12 * d);
        root.addView(message, messageParams);

        // ---- do not show again -------------------------------------------
        final CheckBox skip = new CheckBox(activity);
        skip.setText("Do not show again");
        skip.setTextColor(0xFF8B80B8);
        skip.setTextSize(12f);
        LinearLayout.LayoutParams skipParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        skipParams.topMargin = (int) (10 * d);
        root.addView(skip, skipParams);

        // ---- buttons ------------------------------------------------------
        final AlertDialog dialog = new AlertDialog.Builder(activity).create();

        Button join = makeButton(activity, "JOIN CHANNEL", R.drawable.btn_primary, 0xFF07050E, d);
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTelegram(activity);
                rememberSkip(activity, skip.isChecked());
            }
        });
        root.addView(join);

        Button close = makeButton(activity, "CLOSE", R.drawable.btn_ghost, 0xFFEDE9FE, d);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rememberSkip(activity, skip.isChecked());
                dialog.dismiss();
            }
        });
        root.addView(close);

        Button exit = makeButton(activity, "EXIT", R.drawable.btn_danger, 0xFFFF4D6D, d);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rememberSkip(activity, skip.isChecked());
                dialog.dismiss();
                try {
                    activity.finishAffinity();
                } catch (Throwable t) {
                    activity.finish();
                }
            }
        });
        root.addView(exit);

        dialog.setView(root);
        dialog.setCancelable(false);
        try {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        } catch (Throwable ignored) {
        }
        dialog.show();
    }

    /** Opens the Telegram channel in the browser / Telegram app. */
    public static void openTelegram(Context context) {
        if (context == null) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    private static void rememberSkip(Activity activity, boolean skip) {
        if (activity == null || !skip) {
            return;
        }
        try {
            SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_SKIP, true).commit();
        } catch (Throwable ignored) {
        }
    }

    private static Button makeButton(Activity activity, String label, int background,
                                     int textColor, float d) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextSize(13f);
        button.setTextColor(textColor);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundResource(background);
        try {
            button.setAllCaps(false);
        } catch (Throwable ignored) {
        }
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        (int) (46 * d));
        params.topMargin = (int) (10 * d);
        button.setLayoutParams(params);
        return button;
    }
}
