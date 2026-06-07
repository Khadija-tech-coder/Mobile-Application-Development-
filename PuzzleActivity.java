package com.puzzleverse.game;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.puzzleverse.R;

public class PuzzleActivity extends AppCompatActivity {

    // ─── Constants ─────────────────────────────────────────────────────────────
    private static final int SOLVE_COST       = 50;   // coins spent to auto-solve
    private static final int BASE_COIN_REWARD = 10;   // minimum coins for completing a level

    // ─── State ─────────────────────────────────────────────────────────────────
    private boolean isDaily     = false;
    private boolean isDifficult = false;
    private int     levelNumber = 1;
    private int     moves       = 0;
    private boolean solveUsed   = false; // track if auto-solve was used this level

    // ─── Views ─────────────────────────────────────────────────────────────────
    private PuzzleBoardView puzzleBoard;
    private TextView        tvMovesCount;
    private TextView        tvCoinsCount;
    private TextView        tvLevelTitle;
    private TextView        tvBestMoves;
    private View            btnAutoSolve;
    private View            btnReset;
    private View            btnBack;

    // ─── Data ──────────────────────────────────────────────────────────────────
    private GamePreferences prefs;

    // ───────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        prefs = new GamePreferences(this);

        // Read intent extras
        levelNumber = getIntent().getIntExtra("level_number", 1);
        isDifficult = getIntent().getBooleanExtra("is_difficult", false);
        isDaily     = getIntent().getBooleanExtra("is_daily", false);

        // Bind views
        puzzleBoard   = findViewById(R.id.puzzle_board);
        tvMovesCount  = findViewById(R.id.tv_moves_count);
        tvCoinsCount  = findViewById(R.id.tv_coins_count);
        tvLevelTitle  = findViewById(R.id.tv_level_title);
        tvBestMoves   = findViewById(R.id.tv_best_moves);
        btnAutoSolve  = findViewById(R.id.btn_auto_solve);
        btnReset      = findViewById(R.id.btn_reset);
        btnBack       = findViewById(R.id.btn_back);

        // Initialize board for this level
        puzzleBoard.setLevel(levelNumber);

        // Populate HUD
        updateHUD();

        // Board move / win callbacks
        puzzleBoard.setOnMoveListener(new PuzzleBoardView.OnMoveListener() {
            @Override
            public void onMove(int movesCount) {
                moves = movesCount;
                updateMovesDisplay();
            }

            @Override
            public void onWin(int movesCount) {
                moves = movesCount;
                handleLevelWon();
            }
        });

        // Auto-Solve power-up
        btnAutoSolve.setOnClickListener(v -> handleAutoSolve());

        // Reset / Shuffle
        btnReset.setOnClickListener(v -> {
            SoundManager.getInstance(this).playBtnClick();
            confirmRestart();
        });

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Show mode warning (difficult / daily)
        if (isDifficult || isDaily) {
            puzzleBoard.postDelayed(this::showModeWarning, 500);
        }
    }

    private int getGoalMoves() {
        return levelNumber + 8;
    }

    // ─── HUD helpers ───────────────────────────────────────────────────────────

    private void updateHUD() {
        if (tvLevelTitle  != null) tvLevelTitle.setText("Level " + levelNumber);
        if (tvCoinsCount  != null) tvCoinsCount.setText(String.valueOf(prefs.getCoins()));
        updateMovesDisplay();
        updateBestMovesDisplay();
    }

    private void updateMovesDisplay() {
        if (tvMovesCount != null) tvMovesCount.setText(String.valueOf(moves));
    }

    private void updateBestMovesDisplay() {
        if (tvBestMoves == null) return;
        int best = prefs.getBestMoves(levelNumber);
        tvBestMoves.setText(best == Integer.MAX_VALUE ? "—" : String.valueOf(best));
    }

    private void updateCoinsDisplay() {
        if (tvCoinsCount != null) tvCoinsCount.setText(String.valueOf(prefs.getCoins()));
    }

    // ─── Game Logic ────────────────────────────────────────────────────────────

    private void handleLevelWon() {
        // Save level completion
        prefs.setLevelStatus(levelNumber, GamePreferences.STATUS_COMPLETED);
        prefs.unlockNextLevel(levelNumber);

        // Save best moves (only count if player solved it manually)
        if (!solveUsed) {
            prefs.saveBestMoves(levelNumber, moves);
        }

        // Award coins — more coins for fewer moves; no reward if auto-solve was used
        int coinsEarned = 0;
        if (!solveUsed) {
            // Base reward + bonus for efficient solve (max 50 bonus)
            int best = prefs.getBestMoves(levelNumber);
            coinsEarned = BASE_COIN_REWARD * levelNumber;
            // Bonus: if this is the new best, give extra
            if (moves == best) coinsEarned += 20;
        }
        int newTotal = prefs.addCoins(coinsEarned);
        updateCoinsDisplay();

        // Show win dialog
        showWinDialog(coinsEarned, newTotal);
    }

    private void handleAutoSolve() {
        if (prefs.getCoins() < SOLVE_COST) {
            Toast.makeText(this, getString(R.string.not_enough_coins), Toast.LENGTH_SHORT).show();
            return;
        }

        // Confirm spend
        new AlertDialog.Builder(this)
                .setTitle("⚡ Auto-Solve")
                .setMessage("Spend 50 🪙 coins to instantly solve this puzzle?\n\n(No coins are earned when auto-solve is used.)")
                .setPositiveButton("Yes, solve it!", (d, w) -> {
                    boolean spent = prefs.spendCoins(SOLVE_COST);
                    if (spent) {
                        solveUsed = true;
                        updateCoinsDisplay();
                        // Disable button so it can't be used twice
                        btnAutoSolve.setEnabled(false);
                        btnAutoSolve.setAlpha(0.4f);
                        Toast.makeText(this, getString(R.string.solve_activated), Toast.LENGTH_SHORT).show();
                        SoundManager.getInstance(this).playBtnClick();
                        puzzleBoard.solveNow();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRestart() {
        new AlertDialog.Builder(this)
                .setTitle("🔀 Restart Level?")
                .setMessage("This will shuffle the board and reset your move count.")
                .setPositiveButton("Restart", (d, w) -> {
                    moves = 0;
                    solveUsed = false;
                    btnAutoSolve.setEnabled(true);
                    btnAutoSolve.setAlpha(1f);
                    puzzleBoard.reset();
                    updateHUD();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Dialogs ───────────────────────────────────────────────────────────────

    private void showModeWarning() {
        int mode = isDaily     ? ToastHelper.MODE_DAILY
                 : isDifficult ? ToastHelper.MODE_DIFFICULT
                 :               ToastHelper.MODE_NORMAL;
        ToastHelper.showModeToast(this, mode);
    }

    private void showLevelStartDialog() {
        int reward = BASE_COIN_REWARD * levelNumber;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_level_start, null);
        TextView tvReward = dialogView.findViewById(R.id.tv_reward);
        if (tvReward != null) {
            tvReward.setText(getString(R.string.start_level_reward_format, reward));
        }

        new AlertDialog.Builder(this)
                .setTitle("Level " + levelNumber)
                .setMessage(R.string.puzzle_goal_message)
                .setView(dialogView)
                .setPositiveButton(R.string.start, null)
                .setNegativeButton(R.string.back, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showWinDialog(int coinsEarned, int totalCoins) {
        String coinMsg = solveUsed
                ? "\n\n(Auto-solve used — no coins earned this round)"
                : "\n\n🪙 +" + coinsEarned + " coins! Total: " + totalCoins;

        String message = getString(R.string.puzzle_solved_message,
                levelNumber, moves) + coinMsg;

        new AlertDialog.Builder(this)
                .setTitle(R.string.puzzle_solved_title)
                .setMessage(message)
                .setPositiveButton(R.string.next_level, (dialog, which) -> {
                    // Advance to next level
                    levelNumber++;
                    moves = 0;
                    solveUsed = false;

                    // Re-enable auto-solve for the new level
                    btnAutoSolve.setEnabled(true);
                    btnAutoSolve.setAlpha(1f);

                    // Reset board with new level complexity + update HUD
                    puzzleBoard.setLevel(levelNumber);
                    updateHUD();

                    // Show the start dialog for the new level
                    puzzleBoard.postDelayed(() -> showLevelStartDialog(), 300);
                })
                .setNegativeButton(R.string.menu, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh coins whenever we come back to this screen
        updateCoinsDisplay();
    }

    @Override
    public void finish() {
        super.finish();
        TransitionHelper.back(this);
    }
}
