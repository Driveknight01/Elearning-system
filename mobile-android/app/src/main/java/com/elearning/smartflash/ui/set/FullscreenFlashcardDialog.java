package com.elearning.smartflash.ui.set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.FlashcardItemDto;
import com.elearning.smartflash.util.TtsHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen flashcard swipe mode (Known / Still Learning) launched from the preview zoom button.
 */
public class FullscreenFlashcardDialog extends Dialog {

    public interface OnSessionUpdateListener {
        void onProgressUpdated(int notStudiedCount, int learningCount, int knownCount);
    }

    private final List<FlashcardItemDto> allCards;
    private final List<FlashcardItemDto> queue = new ArrayList<>();
    private final List<FlashcardItemDto> learningList = new ArrayList<>();
    private final List<FlashcardItemDto> knownList = new ArrayList<>();
    private final String termLang;
    private final String defLang;
    private final TtsHelper ttsHelper;
    private final OnSessionUpdateListener progressListener;

    private int currentIndex;
    private int flipCount = 0;
    private boolean autoTts = false;
    private boolean isAnimatingSwipe = false;

    private TextView tvCounter;
    private ProgressBar pbProgress;
    private TextView tvLearningBadge;
    private TextView tvKnownBadge;
    private FrameLayout flCard;
    private MaterialCardView cardFront;
    private MaterialCardView cardBack;
    private TextView tvFrontText;
    private TextView tvBackText;
    private ImageButton btnFrontSpeak;
    private ImageButton btnBackSpeak;
    private ImageButton btnAutoTts;
    private TextView tvSwipeStamp;
    private LinearLayout llSummary;
    private TextView tvSummaryStats;
    private MaterialButton btnRetryUnknown;
    private LinearLayout llBottomActions;
    private ValueAnimator flipAnimator;

    public FullscreenFlashcardDialog(@NonNull Activity activity,
                                     @NonNull List<FlashcardItemDto> cards,
                                     int startIndex,
                                     boolean startShowingDef,
                                     String termLang,
                                     String defLang,
                                     TtsHelper ttsHelper,
                                     OnSessionUpdateListener progressListener) {
        super(activity, R.style.Theme_SmartFlash);
        this.allCards = new ArrayList<>(cards);
        this.queue.addAll(cards);
        this.currentIndex = Math.max(0, Math.min(startIndex, Math.max(0, cards.size() - 1)));
        this.flipCount = startShowingDef ? 1 : 0;
        this.termLang = termLang;
        this.defLang = defLang;
        this.ttsHelper = ttsHelper;
        this.progressListener = progressListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fullscreen_flashcards);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int bgColor = ContextCompat.getColor(getContext(), R.color.quizlet_bg);
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }

        bindViews();
        setupListeners();
        renderCurrentCard();
    }

    private void bindViews() {
        tvCounter = findViewById(R.id.tv_fs_counter);
        pbProgress = findViewById(R.id.pb_fs_progress);
        tvLearningBadge = findViewById(R.id.tv_fs_learning_badge);
        tvKnownBadge = findViewById(R.id.tv_fs_known_badge);
        flCard = findViewById(R.id.fl_fs_card);
        cardFront = findViewById(R.id.card_fs_front);
        cardBack = findViewById(R.id.card_fs_back);
        tvFrontText = findViewById(R.id.tv_fs_front_text);
        tvBackText = findViewById(R.id.tv_fs_back_text);
        btnFrontSpeak = findViewById(R.id.btn_fs_front_speak);
        btnBackSpeak = findViewById(R.id.btn_fs_back_speak);
        btnAutoTts = findViewById(R.id.btn_fs_auto_tts);
        tvSwipeStamp = findViewById(R.id.tv_fs_swipe_stamp);
        llSummary = findViewById(R.id.ll_fs_summary);
        tvSummaryStats = findViewById(R.id.tv_fs_summary_stats);
        btnRetryUnknown = findViewById(R.id.btn_fs_retry_unknown);
        llBottomActions = findViewById(R.id.ll_fs_bottom_actions);

        float camDist = 16000f * getContext().getResources().getDisplayMetrics().density;
        flCard.setCameraDistance(camDist);
        cardFront.setCameraDistance(camDist);
        cardBack.setCameraDistance(camDist);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        findViewById(R.id.btn_fs_close).setOnClickListener(v -> dismiss());

        btnAutoTts.setOnClickListener(v -> {
            autoTts = !autoTts;
            btnAutoTts.setColorFilter(ContextCompat.getColor(
                    getContext(),
                    autoTts ? R.color.quizlet_blue : R.color.sf_text_secondary
            ));
            if (autoTts) speakVisibleSide();
        });

        btnFrontSpeak.setOnClickListener(v -> speakCurrentTerm());
        btnBackSpeak.setOnClickListener(v -> speakCurrentDefinition());

        findViewById(R.id.btn_fs_mark_learning).setOnClickListener(v -> swipeOut(false));
        findViewById(R.id.btn_fs_mark_known).setOnClickListener(v -> swipeOut(true));

        btnRetryUnknown.setOnClickListener(v -> {
            queue.clear();
            queue.addAll(learningList);
            learningList.clear();
            currentIndex = 0;
            flipCount = 0;
            renderCurrentCard();
        });

        findViewById(R.id.btn_fs_restart_all).setOnClickListener(v -> {
            queue.clear();
            queue.addAll(allCards);
            learningList.clear();
            knownList.clear();
            currentIndex = 0;
            flipCount = 0;
            notifyProgress();
            renderCurrentCard();
        });

        final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        flCard.setOnTouchListener(new View.OnTouchListener() {
            float downX;
            float downY;
            boolean dragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isAnimatingSwipe || queue.isEmpty() || currentIndex >= queue.size()) {
                    return false;
                }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        dragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                            dragging = true;
                        }
                        if (dragging) {
                            flCard.setTranslationX(dx);
                            flCard.setRotation(dx * 0.04f);
                            updateSwipeStamp(dx);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float totalDx = event.getRawX() - downX;
                        float threshold = Math.max(160f, flCard.getWidth() * 0.28f);
                        if (dragging && Math.abs(totalDx) > threshold) {
                            swipeOut(totalDx > 0);
                        } else if (dragging) {
                            resetCardPosition();
                        } else {
                            flipCard();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void updateSwipeStamp(float dx) {
        float ratio = Math.min(1f, Math.abs(dx) / 180f);
        tvSwipeStamp.setAlpha(ratio);
        if (dx > 0) {
            tvSwipeStamp.setText("ĐÃ THUỘC");
            tvSwipeStamp.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.quizlet_green)));
        } else if (dx < 0) {
            tvSwipeStamp.setText("CHƯA THUỘC");
            tvSwipeStamp.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.quizlet_amber_dark)));
        }
    }

    private void resetCardPosition() {
        tvSwipeStamp.animate().alpha(0f).setDuration(150).start();
        flCard.animate()
                .translationX(0f)
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void swipeOut(boolean known) {
        if (isAnimatingSwipe || currentIndex < 0 || currentIndex >= queue.size()) return;
        isAnimatingSwipe = true;
        updateSwipeStamp(known ? 200f : -200f);

        float targetX = (known ? 1f : -1f) * (flCard.getWidth() > 0 ? flCard.getWidth() * 1.2f : 900f);
        flCard.animate()
                .translationX(targetX)
                .rotation(known ? 14f : -14f)
                .alpha(0f)
                .setDuration(230)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        flCard.animate().setListener(null);
                        FlashcardItemDto card = queue.get(currentIndex);
                        if (known) {
                            knownList.add(card);
                        } else {
                            learningList.add(card);
                        }
                        currentIndex++;
                        flipCount = 0;
                        isAnimatingSwipe = false;
                        notifyProgress();
                        renderCurrentCard();
                    }
                })
                .start();
    }

    private void notifyProgress() {
        if (progressListener != null) {
            int total = allCards.size();
            int known = Math.min(total, knownList.size());
            int learning = Math.min(total - known, learningList.size());
            int notStudied = Math.max(0, total - known - learning);
            progressListener.onProgressUpdated(notStudied, learning, known);
        }
    }

    private void renderCurrentCard() {
        tvLearningBadge.setText(learningList.size() + "  Chưa thuộc");
        tvKnownBadge.setText("Đã thuộc  " + knownList.size());

        if (currentIndex >= queue.size()) {
            flCard.setVisibility(View.GONE);
            llBottomActions.setVisibility(View.GONE);
            llSummary.setVisibility(View.VISIBLE);
            tvCounter.setText("Hoàn tất");
            pbProgress.setProgress(100);
            tvSummaryStats.setText("Đã thuộc: " + knownList.size() + " thẻ  •  Chưa thuộc: " + learningList.size() + " thẻ");
            btnRetryUnknown.setVisibility(learningList.isEmpty() ? View.GONE : View.VISIBLE);
            return;
        }

        llSummary.setVisibility(View.GONE);
        flCard.setVisibility(View.VISIBLE);
        llBottomActions.setVisibility(View.VISIBLE);

        flCard.setTranslationX(0f);
        flCard.setRotation(0f);
        flCard.setAlpha(1f);
        tvSwipeStamp.setAlpha(0f);

        FlashcardItemDto card = queue.get(currentIndex);
        tvCounter.setText((currentIndex + 1) + " / " + queue.size());
        pbProgress.setProgress(Math.round(((currentIndex + 1) * 100f) / Math.max(1, queue.size())));

        tvFrontText.setText(!TextUtils.isEmpty(card.getTerm()) ? card.getTerm() : "");
        tvBackText.setText(!TextUtils.isEmpty(card.getDefinition()) ? card.getDefinition() : "");

        flCard.setRotationX(flipCount * 180f);
        cardBack.setRotationX(180f);
        applyFaceAlpha(flipCount % 2 == 1);

        if (autoTts) {
            speakVisibleSide();
        }
    }

    private void flipCard() {
        if (flipAnimator != null && flipAnimator.isRunning()) return;
        int prev = flipCount;
        flipCount++;
        float from = prev * 180f;
        float to = flipCount * 180f;

        flCard.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        flipAnimator = ValueAnimator.ofFloat(from, to);
        flipAnimator.setDuration(360L);
        flipAnimator.setInterpolator(new FastOutSlowInInterpolator());
        flipAnimator.addUpdateListener(anim -> {
            float angle = (float) anim.getAnimatedValue();
            flCard.setRotationX(angle);
            float mod = ((angle % 360f) + 360f) % 360f;
            applyFaceAlpha(mod >= 90f && mod <= 270f);
        });
        flipAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                flCard.setRotationX(to);
                flCard.setLayerType(View.LAYER_TYPE_NONE, null);
                if (autoTts) speakVisibleSide();
            }
        });
        flipAnimator.start();
    }

    private void applyFaceAlpha(boolean showBack) {
        cardFront.setAlpha(showBack ? 0f : 1f);
        cardBack.setAlpha(showBack ? 1f : 0f);
        btnFrontSpeak.setClickable(!showBack);
        btnBackSpeak.setClickable(showBack);
    }

    private void speakVisibleSide() {
        if (flipCount % 2 == 1) {
            speakCurrentDefinition();
        } else {
            speakCurrentTerm();
        }
    }

    private void speakCurrentTerm() {
        if (currentIndex < 0 || currentIndex >= queue.size() || ttsHelper == null) return;
        String text = queue.get(currentIndex).getTerm();
        if (!TextUtils.isEmpty(text)) {
            ttsHelper.speak(text, termLang != null ? termLang : "en");
        }
    }

    private void speakCurrentDefinition() {
        if (currentIndex < 0 || currentIndex >= queue.size() || ttsHelper == null) return;
        String text = queue.get(currentIndex).getDefinition();
        if (!TextUtils.isEmpty(text)) {
            ttsHelper.speak(text, defLang != null ? defLang : "vi");
        }
    }
}
