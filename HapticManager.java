package com.puzzleverse.game;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class HapticManager {

    private final Vibrator vibrator;
    private final GamePreferences prefs;

    public HapticManager(Context context, GamePreferences prefs) {
        this.prefs = prefs;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void vibrateSnap() {
        if (!enabled()) return;
        vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    public void vibrateSuccess() {
        if (!enabled()) return;
        long[] pattern = {0, 50, 30, 80};
        vibrate(VibrationEffect.createWaveform(pattern, -1));
    }

    private boolean enabled() {
        return prefs.isHapticEnabled();
    }

    private void vibrate(VibrationEffect effect) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(effect);
        }
    }
}
