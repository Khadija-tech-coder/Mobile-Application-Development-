package com.puzzleverse.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.puzzleverse.R;
import java.util.ArrayList;
import java.util.List;

public class LevelSelectActivity extends AppCompatActivity {

    private RecyclerView rvLevels;
    private LevelAdapter adapter;
    private GamePreferences prefs;
    private TextView tvCoinsLs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        prefs    = new GamePreferences(this);
        rvLevels = findViewById(R.id.rv_levels);
        tvCoinsLs = findViewById(R.id.tv_coins_ls);

        rvLevels.setLayoutManager(new GridLayoutManager(this, 3));

        // Back button
        View btnBack = findViewById(R.id.btn_back_ls);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                SoundManager.getInstance(this).playBtnClick();
                finish();
                TransitionHelper.back(this);
            });
        }

        loadLevels();
        refreshCoins();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh both levels and coin balance when returning from PuzzleActivity
        loadLevels();
        refreshCoins();
    }

    private void refreshCoins() {
        if (tvCoinsLs != null) {
            tvCoinsLs.setText(String.valueOf(prefs.getCoins()));
        }
    }

    private void loadLevels() {
        List<LevelItem> levels = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            int status = prefs.getLevelStatus(i);
            int bestMoves = prefs.getBestMoves(i);
            levels.add(new LevelItem(i, status, i % 5 == 0, bestMoves));
        }
        adapter = new LevelAdapter(levels);
        rvLevels.setAdapter(adapter);
    }

    // ─── Data model ────────────────────────────────────────────────────────────

    static class LevelItem {
        int number;
        int status;
        boolean isDifficult;
        int bestMoves; // Integer.MAX_VALUE = never completed

        LevelItem(int number, int status, boolean isDifficult, int bestMoves) {
            this.number     = number;
            this.status     = status;
            this.isDifficult = isDifficult;
            this.bestMoves  = bestMoves;
        }
    }

    // ─── Adapter ───────────────────────────────────────────────────────────────

    class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.ViewHolder> {
        private final List<LevelItem> levels;

        LevelAdapter(List<LevelItem> levels) {
            this.levels = levels;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_level_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(levels.get(position));
        }

        @Override
        public int getItemCount() {
            return levels.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber, tvLockIcon, tvBadge, tvBestMovesCard;
            ImageView ivThumb;
            View cardRoot, lockOverlay;

            ViewHolder(View itemView) {
                super(itemView);
                tvNumber       = itemView.findViewById(R.id.tv_level_number);
                tvLockIcon     = itemView.findViewById(R.id.tv_lock_icon);
                tvBadge        = itemView.findViewById(R.id.tv_level_badge);
                ivThumb        = itemView.findViewById(R.id.iv_level_thumb);
                cardRoot       = itemView.findViewById(R.id.card_level_root);
                lockOverlay    = itemView.findViewById(R.id.lock_overlay);
                tvBestMovesCard = itemView.findViewById(R.id.tv_best_moves_card);
            }

            void bind(LevelItem item) {
                tvNumber.setText("Level " + item.number);
                ivThumb.setBackgroundColor(getColor(R.color.aurora_panel_dark));

                if (item.status == GamePreferences.STATUS_LOCKED) {
                    cardRoot.setBackgroundResource(R.drawable.bg_aurora_level_locked);
                    lockOverlay.setVisibility(View.VISIBLE);
                    tvLockIcon.setVisibility(View.VISIBLE);
                    tvBadge.setVisibility(View.GONE);
                    if (tvBestMovesCard != null) tvBestMovesCard.setVisibility(View.GONE);
                    itemView.setOnClickListener(null);
                } else {
                    // Unlocked or Completed
                    boolean completed = item.status == GamePreferences.STATUS_COMPLETED;
                    cardRoot.setBackgroundResource(
                            completed ? R.drawable.bg_aurora_daily_unlocked
                                      : R.drawable.bg_aurora_level_normal);
                    lockOverlay.setVisibility(View.GONE);
                    tvLockIcon.setVisibility(View.GONE);

                    // Badge: HARD for difficult levels, ✓ for completed
                    if (item.isDifficult) {
                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText("HARD");
                    } else if (completed) {
                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText("✓");
                    } else {
                        tvBadge.setVisibility(View.GONE);
                    }

                    // Best moves chip (only if level was completed)
                    if (tvBestMovesCard != null) {
                        if (completed && item.bestMoves != Integer.MAX_VALUE) {
                            tvBestMovesCard.setVisibility(View.VISIBLE);
                            tvBestMovesCard.setText("Best: " + item.bestMoves);
                        } else {
                            tvBestMovesCard.setVisibility(View.GONE);
                        }
                    }

                    itemView.setOnClickListener(v -> {
                        SoundManager.getInstance(LevelSelectActivity.this).playBtnClick();
                        Intent intent = new Intent(LevelSelectActivity.this, PuzzleActivity.class);
                        intent.putExtra("level_number", item.number);
                        intent.putExtra("is_difficult", item.isDifficult);
                        startActivity(intent);
                        TransitionHelper.forward(LevelSelectActivity.this);
                    });
                }
            }
        }
    }
}
