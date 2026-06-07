package com.puzzleverse.game;

import android.app.Activity;
import com.example.puzzleverse.R;

public class TransitionHelper {

    // Forward — slide in from right
    public static void forward(Activity a) {
        a.overridePendingTransition(
                R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // Back — slide in from left
    public static void back(Activity a) {
        a.overridePendingTransition(
                R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // Modal — slide up from bottom
    public static void modal(Activity a) {
        a.overridePendingTransition(
                R.anim.slide_in_bottom, R.anim.slide_out_top);
    }
}
