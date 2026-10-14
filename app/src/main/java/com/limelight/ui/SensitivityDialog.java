package com.limelight.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.binding.input.kbm.KbmController;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * In-stream panel for adjusting controller and touch mouse sensitivity.
 * Changes apply immediately and are saved for future streams.
 */
public class SensitivityDialog {
    private interface ValueListener {
        void onValue(int value);
    }

    public static void show(final Activity activity,
                            final PreferenceConfiguration prefConfig,
                            final KbmController kbmController) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        int padding = dp(activity, 20);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, dp(activity, 8), padding, 0);

        if (kbmController != null) {
            addSlider(activity, layout, "Camera sensitivity (right stick)", "px/s", 100, 5000, 50,
                    prefs.getInt(KbmController.PREF_CAMERA_SPEED, KbmController.DEFAULT_CAMERA_SPEED),
                    new ValueListener() {
                        @Override
                        public void onValue(int value) {
                            prefs.edit().putInt(KbmController.PREF_CAMERA_SPEED, value).apply();
                            kbmController.reloadConfig();
                        }
                    });
            addSlider(activity, layout, "Cursor sensitivity (right stick, cursor mode)", "px/s", 100, 5000, 50,
                    prefs.getInt(KbmController.PREF_CURSOR_SPEED, KbmController.DEFAULT_CURSOR_SPEED),
                    new ValueListener() {
                        @Override
                        public void onValue(int value) {
                            prefs.edit().putInt(KbmController.PREF_CURSOR_SPEED, value).apply();
                            kbmController.reloadConfig();
                        }
                    });
            addSlider(activity, layout, "Movement stick deadzone", "%", 5, 90, 1,
                    prefs.getInt(KbmController.PREF_MOVE_DEADZONE, KbmController.DEFAULT_MOVE_DEADZONE),
                    new ValueListener() {
                        @Override
                        public void onValue(int value) {
                            prefs.edit().putInt(KbmController.PREF_MOVE_DEADZONE, value).apply();
                            kbmController.reloadConfig();
                        }
                    });

            CheckBox invertY = new CheckBox(activity);
            invertY.setText("Invert camera Y axis");
            invertY.setChecked(prefs.getBoolean(KbmController.PREF_INVERT_Y, KbmController.DEFAULT_INVERT_Y));
            invertY.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean(KbmController.PREF_INVERT_Y, isChecked).apply();
                    kbmController.reloadConfig();
                }
            });
            layout.addView(invertY);
        }

        addSlider(activity, layout, "Touch camera sensitivity (drag)", "%", 25, 400, 5,
                prefConfig.touchMouseSensitivity,
                new ValueListener() {
                    @Override
                    public void onValue(int value) {
                        // Touch contexts read this on every new touch
                        prefConfig.touchMouseSensitivity = value;
                        prefs.edit().putInt(PreferenceConfiguration.TOUCH_MOUSE_SENSITIVITY_PREF_STRING, value).apply();
                    }
                });

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(layout);

        new AlertDialog.Builder(activity)
                .setTitle("Sensitivity")
                .setView(scrollView)
                .setPositiveButton("Done", null)
                .show();
    }

    private static void addSlider(Activity activity, LinearLayout layout,
                                  final String title, final String unit,
                                  final int min, int max, final int step, int value,
                                  final ValueListener listener) {
        final TextView label = new TextView(activity);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setPadding(0, dp(activity, 12), 0, 0);
        layout.addView(label);

        SeekBar seekBar = new SeekBar(activity);
        seekBar.setMax((max - min) / step);
        int clamped = Math.max(min, Math.min(max, value));
        seekBar.setProgress((clamped - min) / step);
        label.setText(title + ": " + clamped + " " + unit);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = min + progress * step;
                label.setText(title + ": " + newValue + " " + unit);
                if (fromUser) {
                    listener.onValue(newValue);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        layout.addView(seekBar);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                activity.getResources().getDisplayMetrics()));
    }
}
