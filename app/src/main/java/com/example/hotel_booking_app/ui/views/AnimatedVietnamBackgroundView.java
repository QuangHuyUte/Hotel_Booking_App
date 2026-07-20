package com.example.hotel_booking_app.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.hotel_booking_app.R;

public class AnimatedVietnamBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path borderPath = new Path();
    private final RectF rect = new RectF();
    private final RectF mapRect = new RectF();
    private final long cycleMs = 36000L;
    private final long openingMs = 3200L;
    private long startMs;
    private Bitmap logoBitmap;

    public AnimatedVietnamBackgroundView(Context context) {
        super(context);
        init();
    }

    public AnimatedVietnamBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedVietnamBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        logoPaint.setFilterBitmap(true);
        glowPaint.setFilterBitmap(true);
        logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.vietnam_red_logo);
        startMs = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float progress = (System.currentTimeMillis() % cycleMs) / (float) cycleMs;
        drawSeaGradient(canvas);
        drawSeaWaves(canvas, progress);
        drawVietnamLogo(canvas, progress);
        drawBorderHighlight(canvas, progress);
        drawIslandLabels(canvas, progress);
        drawDashedRoutes(canvas, progress);
        drawMovingTravelIcons(canvas, progress);
        drawLocationPins(canvas, progress);
        postInvalidateOnAnimation();
    }

    private void drawSeaGradient(Canvas canvas) {
        paint.setShader(new LinearGradient(
                0, 0, getWidth(), getHeight(),
                new int[]{Color.rgb(8, 75, 148), Color.rgb(35, 137, 196), Color.rgb(154, 219, 231)},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
        paint.setColor(Color.argb(32, 255, 255, 255));
        canvas.drawRect(0, 0, getWidth(), getHeight() * 0.18f, paint);
    }

    private void drawSeaWaves(Canvas canvas, float progress) {
        strokePaint.setStrokeWidth(dp(1.4f));
        strokePaint.setPathEffect(null);
        for (int line = 0; line < 8; line++) {
            path.reset();
            float baseY = getHeight() * (0.15f + line * 0.105f);
            float offset = progress * getWidth() * (0.34f + line * 0.035f);
            for (float x = -getWidth() * 0.25f; x <= getWidth() * 1.25f; x += dp(12)) {
                float y = baseY + (float) Math.sin((x + offset) / dp(34) + line) * dp(4 + line % 3);
                if (x <= -getWidth() * 0.24f) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            strokePaint.setColor(Color.argb(18 + line * 4, 255, 255, 255));
            canvas.drawPath(path, strokePaint);
        }
    }

    private void drawVietnamLogo(Canvas canvas, float progress) {
        if (logoBitmap == null) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        float pulse = 0.99f + 0.01f * (float) Math.sin(progress * Math.PI * 2f);
        float targetH = h * 0.86f;
        float targetW = logoBitmap.getWidth() * (targetH / logoBitmap.getHeight());
        if (targetW > w * 0.78f) {
            targetW = w * 0.78f;
            targetH = logoBitmap.getHeight() * (targetW / logoBitmap.getWidth());
        }
        float left = (w - targetW) / 2f - w * 0.02f;
        float top = (h - targetH) / 2f + h * 0.01f;
        rect.set(left, top, left + targetW, top + targetH);
        mapRect.set(rect);

        float opening = 1f - Math.min(1f, (System.currentTimeMillis() - startMs) / (float) openingMs);
        if (opening > 0.02f) {
            float inflate = dp(8 + 10 * opening);
            rect.inset(-inflate, -inflate);
            glowPaint.setAlpha((int) (120 * opening));
            ColorFilter oldFilter = glowPaint.getColorFilter();
            glowPaint.setColorFilter(new PorterDuffColorFilter(0xFFFFD54A, PorterDuff.Mode.SRC_ATOP));
            canvas.drawBitmap(logoBitmap, null, rect, glowPaint);
            glowPaint.setColorFilter(oldFilter);
            rect.set(mapRect);
        }

        logoPaint.setAlpha((int) (238 * pulse));
        canvas.drawBitmap(logoBitmap, null, rect, logoPaint);
    }

    private void drawBorderHighlight(Canvas canvas, float progress) {
        if (mapRect.isEmpty()) {
            return;
        }
        buildBorderPath();
        float opening = 1f - Math.min(1f, (System.currentTimeMillis() - startMs) / (float) openingMs);
        int alpha = opening > 0.02f ? (int) (245 * opening) : 120;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(opening > 0.02f ? 3.2f : 1.6f));
        strokePaint.setColor(Color.argb(alpha, 255, 215, 74));
        strokePaint.setPathEffect(new DashPathEffect(new float[]{dp(16), dp(10)}, -progress * dp(96)));
        canvas.drawPath(borderPath, strokePaint);
        strokePaint.setPathEffect(null);
    }

    private void buildBorderPath() {
        float l = mapRect.left;
        float t = mapRect.top;
        float w = mapRect.width();
        float h = mapRect.height();
        borderPath.reset();
        borderPath.moveTo(l + w * 0.48f, t + h * 0.04f);
        borderPath.cubicTo(l + w * 0.36f, t + h * 0.12f, l + w * 0.42f, t + h * 0.24f, l + w * 0.50f, t + h * 0.30f);
        borderPath.cubicTo(l + w * 0.59f, t + h * 0.38f, l + w * 0.50f, t + h * 0.47f, l + w * 0.58f, t + h * 0.56f);
        borderPath.cubicTo(l + w * 0.68f, t + h * 0.68f, l + w * 0.55f, t + h * 0.78f, l + w * 0.50f, t + h * 0.91f);
        borderPath.cubicTo(l + w * 0.43f, t + h * 0.77f, l + w * 0.40f, t + h * 0.64f, l + w * 0.43f, t + h * 0.52f);
        borderPath.cubicTo(l + w * 0.45f, t + h * 0.40f, l + w * 0.34f, t + h * 0.28f, l + w * 0.38f, t + h * 0.16f);
        borderPath.cubicTo(l + w * 0.40f, t + h * 0.09f, l + w * 0.43f, t + h * 0.06f, l + w * 0.48f, t + h * 0.04f);
    }

    private void drawIslandLabels(Canvas canvas, float progress) {
        if (mapRect.isEmpty()) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        drawIslandCluster(canvas, w * 0.78f, h * 0.43f, "Hoang Sa", progress, 0.1f);
        drawIslandCluster(canvas, w * 0.73f, h * 0.68f, "Truong Sa", progress, 0.52f);
    }

    private void drawIslandCluster(Canvas canvas, float x, float y, String label, float progress, float phase) {
        float pulse = 0.55f + 0.45f * (float) Math.sin((progress + phase) * Math.PI * 2f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (112 + 42 * pulse), 255, 255, 255));
        canvas.drawCircle(x, y, dp(2.2f), paint);
        canvas.drawCircle(x + dp(8), y + dp(7), dp(1.8f), paint);
        canvas.drawCircle(x - dp(6), y + dp(10), dp(1.5f), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(9));
        paint.setFakeBoldText(true);
        canvas.drawText(label, x, y + dp(26), paint);
        paint.setFakeBoldText(false);
    }

    private void drawDashedRoutes(Canvas canvas, float progress) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setColor(Color.argb(160, 255, 255, 255));
        strokePaint.setPathEffect(new DashPathEffect(new float[]{dp(8), dp(8)}, progress * dp(36)));
        drawCurve(canvas, 0.18f, 0.78f, 0.30f, 0.54f, 0.48f, 0.58f);
        drawCurve(canvas, 0.54f, 0.25f, 0.74f, 0.28f, 0.88f, 0.12f);
        drawCurve(canvas, 0.62f, 0.72f, 0.80f, 0.86f, 0.90f, 0.66f);
        drawCurve(canvas, 0.58f, 0.62f, 0.80f, 0.56f, 0.88f, 0.43f);
        strokePaint.setPathEffect(null);
    }

    private void drawCurve(Canvas canvas, float sx, float sy, float cx, float cy, float ex, float ey) {
        path.reset();
        path.moveTo(getWidth() * sx, getHeight() * sy);
        path.quadTo(getWidth() * cx, getHeight() * cy, getWidth() * ex, getHeight() * ey);
        canvas.drawPath(path, strokePaint);
    }

    private void drawLocationPins(Canvas canvas, float progress) {
        float w = getWidth();
        float h = getHeight();
        if (!mapRect.isEmpty()) {
            drawLocationPin(canvas, mapRect.left + mapRect.width() * 0.44f, mapRect.top + mapRect.height() * 0.14f, "Ha Noi", "city stay", progress, 0.10f, 0xFF1D4ED8, 0xFFDBEAFE);
            drawLocationPin(canvas, mapRect.left + mapRect.width() * 0.48f, mapRect.top + mapRect.height() * 0.79f, "Sai Gon", "hotel", progress, 0.45f, 0xFF0F766E, 0xFFCFFAFE);
            drawLocationPin(canvas, mapRect.left + mapRect.width() * 0.56f, mapRect.top + mapRect.height() * 0.83f, "Vung Tau", "beach", progress, 0.72f, 0xFFF59E0B, 0xFFFFF7CC);
            return;
        }
        drawLocationPin(canvas, w * 0.46f, h * 0.25f, "Ha Noi", "city stay", progress, 0.10f, 0xFF1D4ED8, 0xFFDBEAFE);
        drawLocationPin(canvas, w * 0.48f, h * 0.68f, "Sai Gon", "hotel", progress, 0.45f, 0xFF0F766E, 0xFFCFFAFE);
        drawLocationPin(canvas, w * 0.56f, h * 0.73f, "Vung Tau", "beach", progress, 0.72f, 0xFFF59E0B, 0xFFFFF7CC);
    }

    private void drawLocationPin(Canvas canvas, float cx, float cy, String label, String subtitle, float progress, float phase, int fillColor, int accentColor) {
        float pulse = 0.5f + 0.5f * (float) Math.sin((progress + phase) * Math.PI * 2f);
        float outer = dp(15f + 7f * pulse);
        float inner = dp(8f + 2f * pulse);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(52, 255, 255, 255));
        canvas.drawCircle(cx, cy, outer + dp(9), paint);

        paint.setColor(fillColor);
        canvas.drawCircle(cx, cy, outer, paint);

        strokePaint.setPathEffect(null);
        strokePaint.setStrokeWidth(dp(2f));
        strokePaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, outer, strokePaint);

        rect.set(cx - inner, cy - inner, cx + inner, cy + inner);
        paint.setColor(Color.WHITE);
        canvas.drawOval(rect, paint);
        paint.setColor(accentColor);
        canvas.drawCircle(cx, cy, inner * 0.82f, paint);
        drawTinyBed(canvas, cx, cy, inner * 0.58f, fillColor);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(10));
        canvas.drawText(label, cx, cy + dp(36), paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(dp(8));
        canvas.drawText(subtitle, cx, cy + dp(48), paint);
    }

    private void drawMovingTravelIcons(Canvas canvas, float progress) {
        drawFlightPath(canvas, progress, 0.12f, 0.78f, 0.82f, 0.18f);
        drawFlightPath(canvas, (progress + 0.45f) % 1f, 0.86f, 0.72f, 0.18f, 0.23f);
        drawBoat(canvas, (progress + 0.18f) % 1f, 0.28f, 0.74f, 1f);
        drawBoat(canvas, (progress + 0.58f) % 1f, 0.82f, 0.46f, -1f);
        drawPriceBubble(canvas, (progress + 0.10f) % 1f, "650K", 0.16f, 0.36f);
        drawPriceBubble(canvas, (progress + 0.48f) % 1f, "920K", 0.72f, 0.56f);
        drawBedIcon(canvas, (progress + 0.72f) % 1f, 0.28f, 0.74f);
    }

    private void drawFlightPath(Canvas canvas, float progress, float startX, float startY, float endX, float endY) {
        float w = getWidth();
        float h = getHeight();
        float sx = w * startX;
        float sy = h * startY;
        float ex = w * endX;
        float ey = h * endY;
        float cx = (sx + ex) / 2f;
        float cy = Math.min(sy, ey) - h * 0.1f;

        float t = ease(progress);
        float x = quadratic(sx, cx, ex, t);
        float y = quadratic(sy, cy, ey, t);
        drawPlane(canvas, x, y, angleOnCurve(sx, sy, cx, cy, ex, ey, t));
    }

    private void drawPlane(Canvas canvas, float x, float y, float degrees) {
        canvas.save();
        canvas.rotate(degrees, x, y);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(220, 255, 255, 255));
        path.reset();
        path.moveTo(x + dp(11), y);
        path.lineTo(x - dp(8), y - dp(5));
        path.lineTo(x - dp(3), y);
        path.lineTo(x - dp(8), y + dp(5));
        path.close();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private void drawBoat(Canvas canvas, float progress, float baseX, float baseY, float direction) {
        float x = getWidth() * baseX + (progress - 0.5f) * getWidth() * 0.24f * direction;
        float y = getHeight() * baseY + (float) Math.sin(progress * Math.PI * 2f) * dp(10);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(218, 255, 255, 255));
        path.reset();
        path.moveTo(x - dp(16), y + dp(5));
        path.lineTo(x + dp(14), y + dp(5));
        path.lineTo(x + dp(8), y + dp(12));
        path.lineTo(x - dp(10), y + dp(12));
        path.close();
        canvas.drawPath(path, paint);
        rect.set(x - dp(8), y - dp(5), x + dp(8), y + dp(5));
        canvas.drawRect(rect, paint);

        strokePaint.setColor(Color.argb(132, 255, 255, 255));
        strokePaint.setStrokeWidth(dp(1.4f));
        strokePaint.setPathEffect(null);
        path.reset();
        path.moveTo(x - dp(22), y + dp(18));
        path.cubicTo(x - dp(12), y + dp(13), x - dp(4), y + dp(23), x + dp(8), y + dp(17));
        path.cubicTo(x + dp(16), y + dp(13), x + dp(22), y + dp(18), x + dp(28), y + dp(15));
        canvas.drawPath(path, strokePaint);
    }

    private void drawPriceBubble(Canvas canvas, float progress, String label, float baseX, float baseY) {
        float x = getWidth() * baseX + (progress - 0.5f) * getWidth() * 0.18f;
        float y = getHeight() * baseY + (float) Math.sin(progress * Math.PI * 2f) * dp(16);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(52, 255, 255, 255));
        rect.set(x - dp(31), y - dp(15), x + dp(31), y + dp(15));
        canvas.drawRoundRect(rect, dp(15), dp(15), paint);
        strokePaint.setColor(Color.argb(130, 255, 255, 255));
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setPathEffect(null);
        canvas.drawRoundRect(rect, dp(15), dp(15), strokePaint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(11));
        paint.setFakeBoldText(true);
        canvas.drawText(label, x - dp(18), y + dp(4), paint);
        paint.setFakeBoldText(false);
    }

    private void drawBedIcon(Canvas canvas, float progress, float baseX, float baseY) {
        float x = getWidth() * baseX + (float) Math.cos(progress * Math.PI * 2f) * dp(22);
        float y = getHeight() * baseY + (float) Math.sin(progress * Math.PI * 2f) * dp(12);
        strokePaint.setColor(Color.argb(180, 255, 255, 255));
        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setPathEffect(null);
        rect.set(x - dp(18), y - dp(4), x + dp(18), y + dp(10));
        canvas.drawRoundRect(rect, dp(4), dp(4), strokePaint);
        canvas.drawLine(x - dp(18), y - dp(14), x - dp(18), y + dp(12), strokePaint);
        canvas.drawLine(x + dp(18), y + dp(12), x + dp(18), y + dp(4), strokePaint);
        rect.set(x - dp(14), y - dp(14), x - dp(2), y - dp(4));
        canvas.drawRoundRect(rect, dp(3), dp(3), strokePaint);
    }

    private void drawTinyBed(Canvas canvas, float x, float y, float size, int color) {
        strokePaint.setColor(color);
        strokePaint.setStrokeWidth(dp(1.2f));
        strokePaint.setPathEffect(null);
        rect.set(x - size, y - size * 0.18f, x + size, y + size * 0.42f);
        canvas.drawRoundRect(rect, dp(2), dp(2), strokePaint);
        canvas.drawLine(x - size, y - size * 0.75f, x - size, y + size * 0.55f, strokePaint);
        rect.set(x - size * 0.75f, y - size * 0.72f, x - size * 0.12f, y - size * 0.18f);
        canvas.drawRoundRect(rect, dp(1.5f), dp(1.5f), strokePaint);
    }

    private float ease(float t) {
        return t * t * (3f - 2f * t);
    }

    private float quadratic(float a, float b, float c, float t) {
        return (1 - t) * (1 - t) * a + 2 * (1 - t) * t * b + t * t * c;
    }

    private float angleOnCurve(float sx, float sy, float cx, float cy, float ex, float ey, float t) {
        float dx = 2 * (1 - t) * (cx - sx) + 2 * t * (ex - cx);
        float dy = 2 * (1 - t) * (cy - sy) + 2 * t * (ey - cy);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
