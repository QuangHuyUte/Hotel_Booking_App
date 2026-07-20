package com.example.hotel_booking_app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ProfileDecorView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final long cycleMs = 18000L;

    public ProfileDecorView(Context context) {
        super(context);
        init();
    }

    public ProfileDecorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProfileDecorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float progress = (System.currentTimeMillis() % cycleMs) / (float) cycleMs;
        drawSoftGreenWash(canvas);
        drawVines(canvas, progress);
        drawFloatingBoats(canvas, progress);
        drawFlowers(canvas, progress);
        postInvalidateOnAnimation();
    }

    private void drawSoftGreenWash(Canvas canvas) {
        paint.setShader(new LinearGradient(
                0, 0, getWidth(), getHeight(),
                new int[]{0x442C7FB8, 0x2214789E, 0x110A3D62},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }

    private void drawVines(Canvas canvas, float progress) {
        drawWave(canvas, 0.20f, progress, 0f, 0x55E0F7FA);
        drawWave(canvas, 0.34f, progress, 0.28f, 0x38FFFFFF);
        drawWave(canvas, 0.58f, progress, 0.56f, 0x30B3E5FC);
        drawHanoiSilhouette(canvas, progress);
    }

    private void drawWave(Canvas canvas, float baseY, float progress, float phase, int color) {
        path.reset();
        float y = getHeight() * baseY;
        float offset = (progress + phase) * getWidth() * 0.38f;
        for (float x = -getWidth() * 0.2f; x <= getWidth() * 1.2f; x += dp(18)) {
            float waveY = y + (float) Math.sin((x + offset) / dp(42)) * dp(7);
            if (x <= -getWidth() * 0.19f) {
                path.moveTo(x, waveY);
            } else {
                path.lineTo(x, waveY);
            }
        }
        strokePaint.setStrokeWidth(dp(2.2f));
        strokePaint.setColor(color);
        canvas.drawPath(path, strokePaint);
    }

    private void drawHanoiSilhouette(Canvas canvas, float progress) {
        float base = getHeight() * 0.82f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33212936);
        rect.set(getWidth() * 0.06f, base - dp(40), getWidth() * 0.94f, base + dp(26));
        canvas.drawRoundRect(rect, dp(18), dp(18), paint);

        paint.setColor(0x66FFFFFF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(18));
        canvas.drawText("Hà Nội, Việt Nam", getWidth() * 0.5f, base - dp(12), paint);
        paint.setFakeBoldText(false);

        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setColor(0xAAFFD54A);
        float pulse = 0.65f + 0.35f * (float) Math.sin(progress * Math.PI * 2f);
        canvas.drawCircle(getWidth() * 0.5f, base + dp(7), dp(4 + 3 * pulse), strokePaint);
    }

    private void drawVine(Canvas canvas, float sx, float sy, float cx, float cy, float ex, float ey, float progress, float phase) {
        float sway = (float) Math.sin((progress + phase) * Math.PI * 2f) * dp(10);
        path.reset();
        path.moveTo(getWidth() * sx, getHeight() * sy);
        path.cubicTo(getWidth() * cx + sway, getHeight() * cy,
                getWidth() * (cx + ex) / 2f - sway, getHeight() * (cy + ey) / 2f,
                getWidth() * ex, getHeight() * ey);
        strokePaint.setStrokeWidth(dp(2.2f));
        strokePaint.setColor(0x8AC8FACC);
        canvas.drawPath(path, strokePaint);

        for (int i = 0; i < 8; i++) {
            float t = i / 7f;
            float x = cubic(getWidth() * sx, getWidth() * cx + sway, getWidth() * (cx + ex) / 2f - sway, getWidth() * ex, t);
            float y = cubic(getHeight() * sy, getHeight() * cy, getHeight() * (cy + ey) / 2f, getHeight() * ey, t);
            drawLeaf(canvas, x, y, (i % 2 == 0 ? -1 : 1) * (18 + i * 3));
        }
    }

    private void drawLeaf(Canvas canvas, float x, float y, float degrees) {
        canvas.save();
        canvas.rotate(degrees, x, y);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x99B7F7BE);
        path.reset();
        path.moveTo(x, y);
        path.cubicTo(x + dp(11), y - dp(8), x + dp(18), y + dp(3), x, y + dp(12));
        path.cubicTo(x - dp(4), y + dp(5), x - dp(3), y - dp(3), x, y);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private void drawFloatingBoats(Canvas canvas, float progress) {
        drawBoat(canvas, 0.18f, 0.38f, progress, 1f);
        drawBoat(canvas, 0.78f, 0.73f, (progress + 0.45f) % 1f, -1f);
    }

    private void drawBoat(Canvas canvas, float baseX, float baseY, float progress, float direction) {
        float x = getWidth() * baseX + (progress - 0.5f) * getWidth() * 0.16f * direction;
        float y = getHeight() * baseY + (float) Math.sin(progress * Math.PI * 2f) * dp(8);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x99EAD7A1);
        path.reset();
        path.moveTo(x - dp(18), y + dp(5));
        path.lineTo(x + dp(18), y + dp(5));
        path.lineTo(x + dp(9), y + dp(13));
        path.lineTo(x - dp(11), y + dp(13));
        path.close();
        canvas.drawPath(path, paint);
        rect.set(x - dp(8), y - dp(6), x + dp(8), y + dp(5));
        canvas.drawRect(rect, paint);
    }

    private void drawFlowers(Canvas canvas, float progress) {
        drawFlower(canvas, getWidth() * 0.16f, getHeight() * 0.15f, progress, 0f);
        drawFlower(canvas, getWidth() * 0.83f, getHeight() * 0.24f, progress, 0.33f);
        drawFlower(canvas, getWidth() * 0.70f, getHeight() * 0.88f, progress, 0.66f);
    }

    private void drawFlower(Canvas canvas, float x, float y, float progress, float phase) {
        float pulse = 0.75f + 0.25f * (float) Math.sin((progress + phase) * Math.PI * 2f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xB9FDE68A);
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2 * i / 5.0;
            canvas.drawCircle(
                    x + (float) Math.cos(angle) * dp(7) * pulse,
                    y + (float) Math.sin(angle) * dp(7) * pulse,
                    dp(4.2f),
                    paint
            );
        }
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(2.6f), paint);
    }

    private float cubic(float a, float b, float c, float d, float t) {
        float one = 1f - t;
        return one * one * one * a + 3f * one * one * t * b + 3f * one * t * t * c + t * t * t * d;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
