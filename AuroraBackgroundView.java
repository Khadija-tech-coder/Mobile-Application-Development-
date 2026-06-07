package com.puzzleverse.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class AuroraBackgroundView extends View {
    private Paint paint;
    private float offset = 0;
    private long lastTime = 0;

    public AuroraBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        lastTime = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000f;
        lastTime = currentTime;
        
        offset += deltaTime * 0.1f; // Slow movement
        if (offset > 1.0f) offset -= 1.0f;

        int w = getWidth();
        int h = getHeight();
        
        // Dynamic gradient colors
        int color1 = 0xFF0B0C10;
        int color2 = 0xFF1F2833;
        int color3 = 0xFF0B0C10;
        
        // Calculate dynamic start/end points based on offset
        float x0 = (float) (Math.sin(offset * 2 * Math.PI) * w);
        float y0 = (float) (Math.cos(offset * 2 * Math.PI) * h);
        float x1 = w - x0;
        float y1 = h - y0;

        Shader shader = new LinearGradient(x0, y0, x1, y1,
                new int[]{color1, color2, color3, color2, color1},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1f}, 
                Shader.TileMode.MIRROR);
        
        paint.setShader(shader);
        canvas.drawRect(0, 0, w, h, paint);
        
        invalidate(); // Continuous animation
    }
}
