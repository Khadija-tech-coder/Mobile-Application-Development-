package com.puzzleverse.game;

import android.content.Context;
import android.widget.Toast;

public class ToastHelper {
    public static final int MODE_NORMAL = 0;
    public static final int MODE_DIFFICULT = 1;
    public static final int MODE_DAILY = 2;

    public static void showModeToast(Context context, int mode) {
        String message;
        switch (mode) {
            case MODE_DIFFICULT:
                message = "Difficult Mode: Be careful!";
                break;
            case MODE_DAILY:
                message = "Daily Challenge!";
                break;
            default:
                message = "Good luck!";
                break;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
