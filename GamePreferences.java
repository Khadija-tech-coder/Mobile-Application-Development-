package com.puzzleverse.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class GamePreferences {
    private static final String TAG = "GamePreferences";
    private static final String PREFS_NAME = "puzzle_prefs";
    private static final String KEY_LEVEL_STATUS_PREFIX = "level_status_";
    private static final String KEY_BEST_MOVES_PREFIX = "best_moves_";
    private static final String KEY_COINS = "player_coins";

    public static final int STATUS_LOCKED = 0;
    public static final int STATUS_UNLOCKED = 1;
    public static final int STATUS_COMPLETED = 2;

    private final SharedPreferences prefs;

    public GamePreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ─── Level Status ──────────────────────────────────────────────────────────

    public int getLevelStatus(int level) {
        int defaultStatus = (level == 1) ? STATUS_UNLOCKED : STATUS_LOCKED;
        int status = prefs.getInt(KEY_LEVEL_STATUS_PREFIX + level, defaultStatus);
        Log.d(TAG, "getLevelStatus: Level " + level + " = " + status);
        return status;
    }

    public void setLevelStatus(int level, int status) {
        Log.d(TAG, "setLevelStatus: Level " + level + " -> " + status);
        prefs.edit().putInt(KEY_LEVEL_STATUS_PREFIX + level, status).apply();
    }

    public void unlockNextLevel(int currentLevel) {
        int nextLevel = currentLevel + 1;
        if (getLevelStatus(nextLevel) == STATUS_LOCKED) {
            Log.d(TAG, "unlockNextLevel: Unlocking Level " + nextLevel);
            setLevelStatus(nextLevel, STATUS_UNLOCKED);
        }
    }

    // ─── Best Moves Per Level ──────────────────────────────────────────────────

    /** Returns the best (minimum) moves for a level, or Integer.MAX_VALUE if never played. */
    public int getBestMoves(int level) {
        return prefs.getInt(KEY_BEST_MOVES_PREFIX + level, Integer.MAX_VALUE);
    }

    /** Saves movesCount if it is better (lower) than the stored best. Returns true if a new record. */
    public boolean saveBestMoves(int level, int movesCount) {
        int current = getBestMoves(level);
        if (movesCount < current) {
            prefs.edit().putInt(KEY_BEST_MOVES_PREFIX + level, movesCount).apply();
            Log.d(TAG, "saveBestMoves: Level " + level + " new best = " + movesCount);
            return true;
        }
        return false;
    }

    // ─── Coin Economy ──────────────────────────────────────────────────────────

    /** Returns the player's current coin balance. */
    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    /** Adds coins to the player's balance and returns the new total. */
    public int addCoins(int amount) {
        int newTotal = getCoins() + amount;
        prefs.edit().putInt(KEY_COINS, newTotal).apply();
        Log.d(TAG, "addCoins: +" + amount + " -> total=" + newTotal);
        return newTotal;
    }

    /**
     * Attempts to spend coins. Returns true if successful (balance was sufficient),
     * false if the player cannot afford it.
     */
    public boolean spendCoins(int amount) {
        int current = getCoins();
        if (current < amount) {
            Log.d(TAG, "spendCoins: insufficient balance (" + current + " < " + amount + ")");
            return false;
        }
        int newTotal = current - amount;
        prefs.edit().putInt(KEY_COINS, newTotal).apply();
        Log.d(TAG, "spendCoins: -" + amount + " -> total=" + newTotal);
        return true;
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    public boolean isHapticEnabled() {
        return prefs.getBoolean("haptic_enabled", true);
    }
}
