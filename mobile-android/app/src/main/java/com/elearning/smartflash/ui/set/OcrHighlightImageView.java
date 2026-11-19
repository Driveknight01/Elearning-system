package com.elearning.smartflash.ui.set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom image canvas that renders detected OCR word boxes and lets the user
 * swipe across words to highlight and extract text into Term or Definition.
 */
public class OcrHighlightImageView extends View {

    public static class WordBox {
        public final String text;
        public final Rect bitmapRect;
        final RectF screenRect = new RectF();

        public WordBox(String text, Rect bitmapRect) {
            this.text = text;
            this.bitmapRect = bitmapRect;
        }
    }

    public interface OnTextSelectedListener {
        void onTextSelected(String selectedText);
    }

    private Bitmap bitmap;
    private final List<WordBox> wordBoxes = new ArrayList<>();
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();
    private OnTextSelectedListener selectionListener;

    private final RectF imageDrawRect = new RectF();
    private float scale = 1f;

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OcrHighlightImageView(Context context) {
        super(context);
        init();
    }

    public OcrHighlightImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OcrHighlightImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.parseColor("#4255FF"));
        boxPaint.setStrokeWidth(2.5f);

        selectedFillPaint.setStyle(Paint.Style.FILL);
        selectedFillPaint.setColor(Color.parseColor("#5523B26D"));

        selectedStrokePaint.setStyle(Paint.Style.STROKE);
        selectedStrokePaint.setColor(Color.parseColor("#23B26D"));
        selectedStrokePaint.setStrokeWidth(3.5f);
    }

    public void setOnTextSelectedListener(OnTextSelectedListener listener) {
        this.selectionListener = listener;
    }

    public void setImageAndWords(Bitmap bitmap, List<WordBox> boxes) {
        this.bitmap = bitmap;
        this.wordBoxes.clear();
        if (boxes != null) {
            this.wordBoxes.addAll(boxes);
        }
        this.selectedIndices.clear();
        computeImageBounds();
        invalidate();
    }

    public void clearSelection() {
        selectedIndices.clear();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeImageBounds();
    }

    private void computeImageBounds() {
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        float vw = getWidth();
        float vh = getHeight();
        float bw = bitmap.getWidth();
        float bh = bitmap.getHeight();
        scale = Math.min(vw / bw, vh / bh);
        float drawW = bw * scale;
        float drawH = bh * scale;
        float left = (vw - drawW) / 2f;
        float top = (vh - drawH) / 2f;
        imageDrawRect.set(left, top, left + drawW, top + drawH);

        for (WordBox wb : wordBoxes) {
            wb.screenRect.set(
                    left + wb.bitmapRect.left * scale,
                    top + wb.bitmapRect.top * scale,
                    left + wb.bitmapRect.right * scale,
                    top + wb.bitmapRect.bottom * scale
            );
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) {
            return;
        }
        canvas.drawBitmap(bitmap, null, imageDrawRect, null);
        for (int i = 0; i < wordBoxes.size(); i++) {
            WordBox wb = wordBoxes.get(i);
            if (selectedIndices.contains(i)) {
                canvas.drawRect(wb.screenRect, selectedFillPaint);
                canvas.drawRect(wb.screenRect, selectedStrokePaint);
            } else {
                canvas.drawRect(wb.screenRect, boxPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (wordBoxes.isEmpty()) {
            return super.onTouchEvent(event);
        }
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                selectedIndices.clear();
                hitTestAndSelect(x, y);
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (hitTestAndSelect(x, y)) {
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                if (!selectedIndices.isEmpty() && selectionListener != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Integer idx : selectedIndices) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(wordBoxes.get(idx).text);
                    }
                    selectionListener.onTextSelected(sb.toString().trim());
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private boolean hitTestAndSelect(float x, float y) {
        boolean changed = false;
        float pad = 10f;
        for (int i = 0; i < wordBoxes.size(); i++) {
            RectF r = wordBoxes.get(i).screenRect;
            if (x >= r.left - pad && x <= r.right + pad && y >= r.top - pad && y <= r.bottom + pad) {
                if (selectedIndices.add(i)) {
                    changed = true;
                }
            }
        }
        return changed;
    }
}
