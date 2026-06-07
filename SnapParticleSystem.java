package com.puzzleverse.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnapParticleSystem {

    private static class Particle {
        float x, y, vx, vy;
        float alpha;
        float radius;
        int   color;
    }

    private static final int[] COLORS = {
            Color.parseColor("#66FCF1"),  // aurora cyan
            Color.parseColor("#8B5CF6"),  // aurora violet
            Color.parseColor("#45A29E"),  // aurora teal
            Color.parseColor("#2DD4BF")   // aurora mint
    };

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint();

    public void emit(float x, float y) {
        for (int i = 0; i < 15; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            double angle = random.nextDouble() * 2 * Math.PI;
            float speed = 2 + random.nextFloat() * 5;
            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed);
            p.alpha = 1.0f;
            p.radius = 4 + random.nextFloat() * 4;
            p.color = COLORS[random.nextInt(COLORS.length)];
            particles.add(p);
        }
    }

    public void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 0.04f;
            if (p.alpha <= 0) {
                particles.remove(i);
            }
        }
    }

    public void draw(Canvas canvas) {
        for (Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha((int) (p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }
    }

    public boolean hasParticles() {
        return !particles.isEmpty();
    }
}
