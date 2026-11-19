package com.elearning.smartflash.ui.set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.elearning.smartflash.R;

/**
 * Open-bottom circular arc progress badge matching Quizlet's "Your Progress" counters
 * (Not studied / Still learning / Mastered).
 */
public class CircularProgressBadgeView extends View {

    private static final float START_ANGLE = 140f;
    private static final float MAX_SWEEP_ANGLE = 260f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private int count = 0;
    private int total = 0;
    private int activeTextColor;
    private int mutedTextColor;

    public CircularProgressBadgeView(Context context) {
        super(context);
        init(context);
    }

    public CircularProgressBadgeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircularProgressBadgeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = getResources().getDisplayMetrics().density;
        float strokePx = 3.5f * density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokePx);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(0xFFD8DDF0);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokePx);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(0xFF2E3856);

        activeTextColor = ContextCompat.getColor(context, R.color.sf_text_primary);
        mutedTextColor = ContextCompat.getColor(context, R.color.sf_hint_light);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(14f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setColor(activeTextColor);
    }

    public void setColors(@ColorInt int trackColor, @ColorInt int progressColor) {
        trackPaint.setColor(trackColor);
        progressPaint.setColor(progressColor);
        invalidate();
    }

    public void setProgress(int count, int total) {
        this.count = Math.max(0, count);
        this.total = Math.max(0, total);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float pad = trackPaint.getStrokeWidth() / 2f + 2f;
        arcBounds.set(pad, pad, w - pad, h - pad);

        canvas.drawArc(arcBounds, START_ANGLE, MAX_SWEEP_ANGLE, false, trackPaint);

        if (count > 0 && total > 0) {
            float ratio = Math.min(1f, (float) count / (float) total);
            float sweep = Math.max(14f, MAX_SWEEP_ANGLE * ratio);
            canvas.drawArc(arcBounds, START_ANGLE, sweep, false, progressPaint);
        }

        textPaint.setColor(count > 0 ? activeTextColor : mutedTextColor);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = (h / 2f) - ((fm.ascent + fm.descent) / 2f);
        canvas.drawText(String.valueOf(count), w / 2f, textY, textPaint);
    }
}
