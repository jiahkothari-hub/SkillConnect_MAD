package com.skillconnect.app.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Draws a soft coloured background with the person's initials – used when there is no photo. */
public class AvatarDrawable extends android.graphics.drawable.Drawable {

    private static final int[][] PALETTE = {
            {0xFFE0E7FF, 0xFF3730A3},
            {0xFFDCFCE7, 0xFF166534},
            {0xFFFCE7F3, 0xFF9D174D},
            {0xFFFEF3C7, 0xFF92400E},
            {0xFFE0F2FE, 0xFF075985},
            {0xFFEDE9FE, 0xFF5B21B6},
            {0xFFCCFBF1, 0xFF115E59},
    };

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String initials;

    public AvatarDrawable(String name) {
        initials = initialsOf(name);
        int[] colors = PALETTE[Math.abs((name != null ? name : "").hashCode()) % PALETTE.length];
        bgPaint.setColor(colors[0]);
        textPaint.setColor(colors[1]);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        canvas.drawRect(b, bgPaint);
        textPaint.setTextSize(b.height() * 0.38f);
        float y = b.exactCenterY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(initials, b.exactCenterX(), y, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        bgPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        bgPaint.setColorFilter(colorFilter);
        textPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
        return 160;
    }

    @Override
    public int getIntrinsicHeight() {
        return 160;
    }
}
