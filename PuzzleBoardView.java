package com.puzzleverse.game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PuzzleBoardView extends View {

    public interface OnMoveListener {
        void onMove(int moves);
        void onWin(int moves);
    }

    private int rows = 3;
    private int cols = 3;
    private int numSlots = 9;
    private int[] tiles;
    private int emptyIndex;
    private int moves = 0;
    private OnMoveListener listener;
    
    private Paint tilePaint;
    private Paint textPaint;
    private Paint emptyPaint;
    private float tileSize;
    private float padding = 8f;
    private float cornerRadius = 24f;

    private SnapParticleSystem particleSystem;
    private HapticManager hapticManager;
    
    // Animation state
    private int animatingIndex = -1;
    private float animOffsetX = 0;
    private float animOffsetY = 0;

    public PuzzleBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        initTiles();

        tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tilePaint.setColor(Color.parseColor("#1F2833"));
        tilePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#66FCF1"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#0B0C10"));
        emptyPaint.setStyle(Paint.Style.FILL);

        particleSystem = new SnapParticleSystem();
        hapticManager = new HapticManager(getContext(), new GamePreferences(getContext()));
        
        setClickable(true);
    }

    private void initTiles() {
        tiles = new int[rows * cols];
        // We only use numSlots. The rest are 0 (but won't be drawn or reachable)
        for (int i = 0; i < numSlots - 1; i++) {
            tiles[i] = i + 1;
        }
        tiles[numSlots - 1] = 0; // empty
        emptyIndex = numSlots - 1;
        
        shuffle();
    }

    public void setLevel(int level) {
        this.numSlots = level + 8;
        this.cols = 3;
        this.rows = (int) Math.ceil(numSlots / 3.0);
        initTiles();
        invalidate();
    }

    private void shuffle() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < numSlots - 1; i++) {
            list.add(i + 1);
        }
        
        do {
            Collections.shuffle(list);
            for (int i = 0; i < list.size(); i++) {
                tiles[i] = list.get(i);
            }
            tiles[numSlots - 1] = 0;
            emptyIndex = numSlots - 1;
        } while (!isSolvable() || isSolved());
        
        moves = 0;
    }

    private boolean isSolvable() {
        int inversions = 0;
        for (int i = 0; i < numSlots - 1; i++) {
            for (int j = i + 1; j < numSlots - 1; j++) {
                if (tiles[i] > 0 && tiles[j] > 0 && tiles[i] > tiles[j]) {
                    inversions++;
                }
            }
        }
        // For simplicity on non-square/dynamic grids, we'll just check inversions.
        // On 3xN grids, this is generally sufficient if width is odd.
        return inversions % 2 == 0;
    }

    private boolean isSolved() {
        if (tiles[numSlots - 1] != 0) return false;
        for (int i = 0; i < numSlots - 1; i++) {
            if (tiles[i] != i + 1) return false;
        }
        return true;
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float availableWidth = w - getPaddingLeft() - getPaddingRight();
        float availableHeight = h - getPaddingTop() - getPaddingBottom();
        tileSize = Math.min(availableWidth / cols, availableHeight / rows);
        textPaint.setTextSize(tileSize * 0.4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float startX = (getWidth() - tileSize * cols) / 2;
        float startY = (getHeight() - tileSize * rows) / 2;

        for (int i = 0; i < numSlots; i++) {
            int row = i / cols;
            int col = i % cols;

            float left = startX + col * tileSize + padding;
            float top = startY + row * tileSize + padding;
            float right = left + tileSize - 2 * padding;
            float bottom = top + tileSize - 2 * padding;

            if (i == animatingIndex) {
                left += animOffsetX;
                right += animOffsetX;
                top += animOffsetY;
                bottom += animOffsetY;
            }

            if (tiles[i] != 0) {
                RectF rect = new RectF(left, top, right, bottom);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tilePaint);
                
                // Draw number
                float textX = left + (right - left) / 2;
                float textY = top + (bottom - top) / 2 - (textPaint.descent() + textPaint.ascent()) / 2;
                canvas.drawText(String.valueOf(tiles[i]), textX, textY, textPaint);
            } else {
                RectF rect = new RectF(left, top, right, bottom);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, emptyPaint);
            }
        }

        particleSystem.update();
        particleSystem.draw(canvas);
        
        if (!particlesEmpty()) {
            invalidate();
        }
    }

    private boolean particlesEmpty() {
        return !particleSystem.hasParticles();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (animatingIndex != -1) return true;

            float startX = (getWidth() - tileSize * cols) / 2;
            float startY = (getHeight() - tileSize * rows) / 2;

            int col = (int) ((event.getX() - startX) / tileSize);
            int row = (int) ((event.getY() - startY) / tileSize);

            if (col >= 0 && col < cols && row >= 0 && row < rows) {
                int index = row * cols + col;
                if (index < numSlots) {
                    tryMove(index);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void tryMove(int index) {
        if (tiles[index] == 0) return;

        int row = index / cols;
        int col = index % cols;
        int emptyRow = emptyIndex / cols;
        int emptyCol = emptyIndex % cols;

        if (Math.abs(row - emptyRow) + Math.abs(col - emptyCol) == 1) {
            animateMove(index, emptyIndex);
        }
    }

    private void animateMove(final int fromIndex, final int toIndex) {
        animatingIndex = fromIndex;
        
        int fromRow = fromIndex / cols;
        int fromCol = fromIndex % cols;
        int toRow = toIndex / cols;
        int toCol = toIndex % cols;

        float targetOffsetX = (toCol - fromCol) * tileSize;
        float targetOffsetY = (toRow - fromRow) * tileSize;

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            animOffsetX = targetOffsetX * fraction;
            animOffsetY = targetOffsetY * fraction;
            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Swap tiles
                tiles[toIndex] = tiles[fromIndex];
                tiles[fromIndex] = 0;
                emptyIndex = fromIndex;
                
                animatingIndex = -1;
                animOffsetX = 0;
                animOffsetY = 0;
                
                moves++;
                if (listener != null) {
                    listener.onMove(moves);
                }

                // Effects
                float startX = (getWidth() - tileSize * cols) / 2;
                float startY = (getHeight() - tileSize * rows) / 2;
                float centerX = (toIndex % cols + 0.5f) * tileSize + startX;
                float centerY = (toIndex / cols + 0.5f) * tileSize + startY;
                particleSystem.emit(centerX, centerY);
                hapticManager.vibrateSnap();
                SoundManager.getInstance(getContext()).playTileSnap();

                if (isSolved()) {
                    SoundManager.getInstance(getContext()).playLevelWin();
                    if (listener != null) {
                        listener.onWin(moves);
                    }
                }
                invalidate();
            }
        });

        animator.start();
    }
    
    public void reset() {
        shuffle();
        invalidate();
    }

    /** Resets only the move counter to 0 without reshuffling (used when advancing levels internally). */
    public void resetMoves() {
        moves = 0;
    }

    /**
     * Auto-solves the current puzzle state using BFS, then animates each move.
     * The onMove/onWin callbacks fire normally so coins & UI update correctly.
     */
    public void solveNow() {
        List<Integer> solution = bfsSolve();
        if (solution == null || solution.isEmpty()) return;
        animateSolution(solution, 0);
    }

    /** BFS from the current tile state to the goal state. Returns a list of tile indices to move. */
    private List<Integer> bfsSolve() {
        if (numSlots > 12) {
            // BFS is too slow for large puzzles. Teleport to solve for now.
            for(int i=0; i<numSlots-1; i++) tiles[i] = i+1;
            tiles[numSlots-1] = 0;
            emptyIndex = numSlots-1;
            
            // Post victory on the UI thread to ensure consistent state
            post(() -> {
                if (listener != null) listener.onWin(moves);
                invalidate();
            });
            return null;
        }
        // Goal: tiles [1,2,3,4,5,6,7,8,0]
        int[] goal = new int[rows * cols];
        for (int i = 0; i < numSlots - 1; i++) goal[i] = i + 1;
        goal[numSlots - 1] = 0;

        // State key: comma-joined array
        String startKey = Arrays.toString(Arrays.copyOf(tiles, numSlots));
        String goalKey  = Arrays.toString(Arrays.copyOf(goal, numSlots));

        if (startKey.equals(goalKey)) return new ArrayList<>();

        // BFS
        ArrayDeque<String> queue = new ArrayDeque<>();
        // parentMap: state -> (parent state, tile index moved to reach this state)
        Map<String, int[]> parentMap = new HashMap<>();
        // int[] = { parentState encoded as reference, movedTileIndex }
        // We store parentState key -> [movedTileIndex, emptyIndex before move]
        // Simpler: store stateKey -> "parentStateKey,movedTileIndex"
        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Integer> movedTile = new HashMap<>();

        queue.add(startKey);
        cameFrom.put(startKey, null);

        // We also need to track the actual array per state for neighbours
        Map<String, int[]> stateArrays = new HashMap<>();
        stateArrays.put(startKey, tiles.clone());

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(goalKey)) {
                // Reconstruct path
                List<Integer> path = new ArrayList<>();
                String s = current;
                while (cameFrom.get(s) != null) {
                    path.add(0, movedTile.get(s));
                    s = cameFrom.get(s);
                }
                return path;
            }

            int[] state = stateArrays.get(current);
            // Find empty index
            int empty = -1;
            for (int i = 0; i < state.length; i++) {
                if (state[i] == 0) { empty = i; break; }
            }

            int emptyRow = empty / cols;
            int emptyCol = empty % cols;
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};

            for (int[] d : dirs) {
                int nr = emptyRow + d[0];
                int nc = emptyCol + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                int neighborIdx = nr * cols + nc;
                if (neighborIdx >= numSlots) continue;

                int[] next = state.clone();
                next[empty] = next[neighborIdx];
                next[neighborIdx] = 0;

                String nextKey = Arrays.toString(Arrays.copyOf(next, numSlots));
                if (cameFrom.containsKey(nextKey)) continue;

                cameFrom.put(nextKey, current);
                movedTile.put(nextKey, neighborIdx); // which tile index was moved into empty
                stateArrays.put(nextKey, next);
                queue.add(nextKey);
            }
        }
        return null; // no solution (shouldn't happen for valid puzzles)
    }

    /** Recursively animates each tile index from the BFS solution list with a small delay between moves. */
    private void animateSolution(final List<Integer> solution, final int step) {
        if (step >= solution.size()) return;
        int tileIndexToMove = solution.get(step);
        // Animate the move; after it completes, schedule next step
        postDelayed(() -> {
            tryMove(tileIndexToMove);
            // Wait for current animation to complete (200ms) + small buffer, then proceed
            postDelayed(() -> animateSolution(solution, step + 1), 280);
        }, 0);
    }
}
