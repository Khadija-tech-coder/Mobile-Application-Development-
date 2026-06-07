package com.puzzleverse.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import com.example.puzzleverse.R;

public class SoundManager {
    private static SoundManager instance;
    private final SoundPool soundPool;
    private final int tileSnapId;
    private final int levelWinId;
    private final int btnClickId;
    private boolean loaded = false;

    private SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        tileSnapId = soundPool.load(context, R.raw.tile_snap, 1);
        levelWinId = soundPool.load(context, R.raw.level_win, 1);
        btnClickId = soundPool.load(context, R.raw.btn_click, 1);

        soundPool.setOnLoadCompleteListener((soundPool1, sampleId, status) -> loaded = true);
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public void playTileSnap() {
        playSound(tileSnapId);
    }

    public void playLevelWin() {
        playSound(levelWinId);
    }

    public void playBtnClick() {
        playSound(btnClickId);
    }

    private void playSound(int soundId) {
        if (loaded) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
}
