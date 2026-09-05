package com.openrosary.app;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * Centralized haptic feedback helper supporting symbol-based customization:
 *   "*"   -> 1x getar (single 50ms pulse)
 *   "**"  -> 2x getar (two 50ms pulses with 100ms pause)
 *   "***" -> 3x getar (three 50ms pulses)
 *   "_" or "~" -> Long sustained pulse (150ms)
 */
public final class HapticHelper {
    private static final String TAG = "HapticHelper";

    private static final long PULSE_DURATION = 50L;
    private static final long LONG_PULSE_DURATION = 150L;
    private static final long PAUSE_DURATION = 100L;

    private static final long[] PATTERN_1X = {0, PULSE_DURATION};
    private static final long[] PATTERN_2X = {0, PULSE_DURATION, PAUSE_DURATION, PULSE_DURATION};
    private static final long[] PATTERN_3X = {0, PULSE_DURATION, PAUSE_DURATION, PULSE_DURATION, PAUSE_DURATION, PULSE_DURATION};
    private static final long[] PATTERN_LONG = {0, LONG_PULSE_DURATION};

    private HapticHelper() {}

    /**
     * Vibrate based on custom prayer symbolic notation.
     * @param context Android context
     * @param symbol Symbolic cue: "*", "**", "***", "[vib:1]", "[vib:2]", "[vib:3]", "~", "_"
     */
    public static void vibrateSymbol(Context context, String symbol) {
        if (context == null || symbol == null) return;
        String s = symbol.trim();
        if (s.contains("***") || s.contains("vib:3") || s.contains("~~~")) {
            vibrate(context, 3);
        } else if (s.contains("**") || s.contains("vib:2") || s.contains("~~")) {
            vibrate(context, 2);
        } else if (s.contains("_") || s.equalsIgnoreCase("long")) {
            vibrateLong(context);
        } else {
            vibrate(context, 1);
        }
    }

    /**
     * Vibrate a specific number of pulses (1x, 2x, or 3x).
     */
    public static void vibrate(Context context, int count) {
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;

            long[] pattern;
            switch (count) {
                case 2:
                    pattern = PATTERN_2X;
                    break;
                case 3:
                    pattern = PATTERN_3X;
                    break;
                case 1:
                default:
                    pattern = PATTERN_1X;
                    break;
            }
            executeVibration(vibrator, pattern);
        } catch (Exception e) {
            Log.e(TAG, "Vibration failed: " + e.getMessage());
        }
    }

    /**
     * Long sustained pulse for solemn moments or blessings.
     */
    public static void vibrateLong(Context context) {
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            executeVibration(vibrator, PATTERN_LONG);
        } catch (Exception e) {
            Log.e(TAG, "Vibration failed: " + e.getMessage());
        }
    }

    private static void executeVibration(Vibrator vibrator, long[] pattern) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }
}
