package com.example.puzzleverse;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.puzzleverse.game.GamePreferences;
import com.puzzleverse.game.LevelSelectActivity;
import com.puzzleverse.game.SoundManager;
import com.puzzleverse.game.TransitionHelper;

public class MainActivity extends AppCompatActivity {

    private GamePreferences prefs;
    private TextView tvCoinsMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs       = new GamePreferences(this);
        tvCoinsMain = findViewById(R.id.tv_coins_main);

        refreshCoins();

        findViewById(R.id.btn_play).setOnClickListener(v -> {
            SoundManager.getInstance(this).playBtnClick();
            startActivity(new Intent(MainActivity.this, LevelSelectActivity.class));
            TransitionHelper.forward(this);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always show the latest coin balance when returning to the main screen
        refreshCoins();
    }

    private void refreshCoins() {
        if (tvCoinsMain != null) {
            tvCoinsMain.setText(String.valueOf(prefs.getCoins()));
        }
    }
}