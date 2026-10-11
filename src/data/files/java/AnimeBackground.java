package com.shadow.injector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/**
 * Anime themed animated background for Shadow Injector.
 *
 * Draws, entirely on a Canvas (no external library, no XML animation):
 *   - a deep violet vertical gradient
 *   - rising neon sparks (violet / cyan) that sway left and right
 *   - falling sakura petals that slowly spin
 *
 * Just add it to any container, e.g:
 * <pre>
 *     ((FrameLayout) findViewById(R.id.frame_root))
 *         .addView(new AnimeBackground(this), 0,
 *                  new FrameLayout.LayoutParams(-1, -1));
 * </pre>
 */
public class AnimeBackground extends View {

    private static final int COLOR_TOP = 0xFF170B31;
    private static final int COLOR_BOTTOM = 0xFF05040C;
    private static final int COLOR_NEON_VIOLET = 0xFFB14CFF;
    private static final int COLOR_NEON_CYAN = 0xFF22D3EE;
    private static final int COLOR_PETAL = 0xFFF58FB8;

    private final ArrayList<Spark> sparks = new ArrayList<Spark>();
    private final ArrayList<Petal> petals = new ArrayList<Petal>();
    private final Random rnd = new Random();

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint petalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private LinearGradient gradient;
    private int vw = 1;
    private int vh = 1;
    private float time;

    public AnimeBackground(Context context) {
        super(context);
    }

    public AnimeBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimeBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        vw = w > 0 ? w : 1;
        vh = h > 0 ? h : 1;
        gradient = new LinearGradient(0f, 0f, vw * 0.45f, vh, COLOR_TOP, COLOR_BOTTOM, Shader.TileMode.CLAMP);
        rebuild();
    }

    private void rebuild() {
        sparks.clear();
        petals.clear();

        if (vw <= 1 || vh <= 1) {
            return;
        }

        int sparkCount = Math.max(24, (vw * vh) / 24000);
        for (int i = 0; i < sparkCount; i++) {
            sparks.add(newSpark(true));
        }

        int petalCount = Math.max(10, vw / 70);
        for (int i = 0; i < petalCount; i++) {
            petals.add(newPetal(true));
        }
    }

    private Spark newSpark(boolean spread) {
        Spark s = new Spark();
        s.x = rnd.nextFloat() * vw;
        s.y = spread ? rnd.nextFloat() * vh : vh + 8f + rnd.nextFloat() * 40f;
        s.r = 1.0f + rnd.nextFloat() * 2.6f;
        s.speed = 0.16f + rnd.nextFloat() * 0.52f;
        s.sway = 4f + rnd.nextFloat() * 16f;
        s.freq = 0.004f + rnd.nextFloat() * 0.010f;
        s.phase = rnd.nextFloat() * 6.283f;
        s.alpha = 40 + rnd.nextInt(170);
        s.cyan = rnd.nextFloat() < 0.35f;
        return s;
    }

    private Petal newPetal(boolean spread) {
        Petal p = new Petal();
        p.x = rnd.nextFloat() * vw;
        p.y = spread ? rnd.nextFloat() * vh : -20f - rnd.nextFloat() * 70f;
        p.size = 4f + rnd.nextFloat() * 6f;
        p.speed = 0.30f + rnd.nextFloat() * 0.80f;
        p.sway = 8f + rnd.nextFloat() * 22f;
        p.freq = 0.006f + rnd.nextFloat() * 0.010f;
        p.phase = rnd.nextFloat() * 6.283f;
        p.spin = (rnd.nextFloat() - 0.5f) * 2.0f;
        p.alpha = 40 + rnd.nextInt(90);
        return p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (gradient == null) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;

        /* 1. background gradient */
        bgPaint.setShader(gradient);
        canvas.drawRect(0f, 0f, vw, vh, bgPaint);
        bgPaint.setShader(null);

        /* 2. rising neon sparks */
        sparkPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < sparks.size(); i++) {
            Spark s = sparks.get(i);
            s.y -= s.speed * density;
            if (s.y < -12f) {
                sparks.set(i, newSpark(false));
                continue;
            }
            float x = s.x + (float) Math.sin(time * s.freq + s.phase) * s.sway * density;
            sparkPaint.setColor(s.cyan ? COLOR_NEON_CYAN : COLOR_NEON_VIOLET);
            sparkPaint.setAlpha(s.alpha);
            canvas.drawCircle(x, s.y, s.r * density, sparkPaint);
        }

        /* 3. falling sakura petals */
        petalPaint.setStyle(Paint.Style.FILL);
        petalPaint.setColor(COLOR_PETAL);
        for (int i = 0; i < petals.size(); i++) {
            Petal p = petals.get(i);
            p.y += p.speed * density;
            p.angle += p.spin;
            if (p.y > vh + 24f) {
                petals.set(i, newPetal(false));
                continue;
            }
            float x = p.x + (float) Math.sin(time * p.freq + p.phase) * p.sway * density;
            petalPaint.setAlpha(p.alpha);
            float w = p.size * density;
            float h = w * 0.62f;
            canvas.save();
            canvas.translate(x, p.y);
            canvas.rotate(p.angle);
            canvas.drawOval(-w, -h, w, h, petalPaint);
            canvas.restore();
        }

        time += 16f;
        if (time > 1000000f) {
            time = 0f;
        }

        postInvalidateDelayed(33L);
    }

    private static class Spark {
        float x;
        float y;
        float r;
        float speed;
        float sway;
        float freq;
        float phase;
        int alpha;
        boolean cyan;
    }

    private static class Petal {
        float x;
        float y;
        float size;
        float speed;
        float sway;
        float freq;
        float phase;
        float spin;
        float angle;
        int alpha;
    }
}
