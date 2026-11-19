package com.elearning.smartflash.ui.set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.FlashcardItemDto;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewPager2 adapter for the Quizlet-style flashcard preview carousel with
 * continuous horizontal-axis (rotationX) 3D flip matching Web {@code rotateX(flipCount * 180deg)}.
 */
public class PreviewDeckAdapter extends RecyclerView.Adapter<PreviewDeckAdapter.DeckCardViewHolder> {

    public interface OnDeckCardListener {
        void onSpeakTerm(FlashcardItemDto card);
        void onSpeakDefinition(FlashcardItemDto card);
        void onOpenFullscreen(int position, boolean showingDefinition);
    }

    private final List<FlashcardItemDto> cards = new ArrayList<>();
    private final Map<Integer, Integer> flipCounts = new HashMap<>();
    private final OnDeckCardListener listener;

    public PreviewDeckAdapter(OnDeckCardListener listener) {
        this.listener = listener;
    }

    public void submitCards(List<FlashcardItemDto> newCards) {
        cards.clear();
        flipCounts.clear();
        if (newCards != null) {
            cards.addAll(newCards);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeckCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview_flashcard, parent, false);
        return new DeckCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckCardViewHolder holder, int position) {
        FlashcardItemDto card = cards.get(position);
        Integer countObj = flipCounts.get(position);
        int flipCount = countObj != null ? countObj : 0;
        holder.bind(card, position, flipCount, flipCounts, listener);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class DeckCardViewHolder extends RecyclerView.ViewHolder {

        private final FrameLayout flipContainer;
        private final MaterialCardView cardFront;
        private final MaterialCardView cardBack;
        private final TextView tvFrontText;
        private final TextView tvBackText;
        private final ImageButton btnFrontSpeak;
        private final ImageButton btnBackSpeak;
        private final ImageButton btnFrontFullscreen;
        private final ImageButton btnBackFullscreen;
        private ValueAnimator activeAnimator;

        DeckCardViewHolder(@NonNull View itemView) {
            super(itemView);
            flipContainer = itemView.findViewById(R.id.fl_card_flip_container);
            cardFront = itemView.findViewById(R.id.card_face_front);
            cardBack = itemView.findViewById(R.id.card_face_back);
            tvFrontText = itemView.findViewById(R.id.tv_front_text);
            tvBackText = itemView.findViewById(R.id.tv_back_text);
            btnFrontSpeak = itemView.findViewById(R.id.btn_front_speak);
            btnBackSpeak = itemView.findViewById(R.id.btn_back_speak);
            btnFrontFullscreen = itemView.findViewById(R.id.btn_front_fullscreen);
            btnBackFullscreen = itemView.findViewById(R.id.btn_back_fullscreen);

            float cameraDistance = 16000f * itemView.getResources().getDisplayMetrics().density;
            flipContainer.setCameraDistance(cameraDistance);
            cardFront.setCameraDistance(cameraDistance);
            cardBack.setCameraDistance(cameraDistance);
        }

        void bind(FlashcardItemDto card,
                  int position,
                  int flipCount,
                  Map<Integer, Integer> flipCounts,
                  OnDeckCardListener listener) {
            if (activeAnimator != null) {
                activeAnimator.cancel();
                activeAnimator = null;
            }

            tvFrontText.setText(!TextUtils.isEmpty(card.getTerm()) ? card.getTerm() : "");
            tvBackText.setText(!TextUtils.isEmpty(card.getDefinition()) ? card.getDefinition() : "");

            boolean showingBack = (flipCount % 2 == 1);
            flipContainer.setRotationX(flipCount * 180f);
            cardBack.setRotationX(180f);
            applyFaceVisibility(showingBack);

            View.OnClickListener flipClick = v -> {
                if (activeAnimator != null && activeAnimator.isRunning()) {
                    return;
                }
                Integer currentObj = flipCounts.get(position);
                int currentCount = currentObj != null ? currentObj : 0;
                int nextCount = currentCount + 1;
                flipCounts.put(position, nextCount);
                animateContinuousFlip(currentCount * 180f, nextCount * 180f);
            };
            cardFront.setOnClickListener(flipClick);
            cardBack.setOnClickListener(flipClick);

            btnFrontSpeak.setOnClickListener(v -> {
                if (listener != null) listener.onSpeakTerm(card);
            });
            btnBackSpeak.setOnClickListener(v -> {
                if (listener != null) listener.onSpeakDefinition(card);
            });

            btnFrontFullscreen.setOnClickListener(v -> {
                if (listener != null) {
                    Integer currentObj = flipCounts.get(position);
                    boolean isBack = currentObj != null && (currentObj % 2 == 1);
                    listener.onOpenFullscreen(position, isBack);
                }
            });
            btnBackFullscreen.setOnClickListener(v -> {
                if (listener != null) {
                    Integer currentObj = flipCounts.get(position);
                    boolean isBack = currentObj != null && (currentObj % 2 == 1);
                    listener.onOpenFullscreen(position, isBack);
                }
            });
        }

        private void applyFaceVisibility(boolean showBack) {
            cardFront.setAlpha(showBack ? 0f : 1f);
            cardBack.setAlpha(showBack ? 1f : 0f);
            cardFront.setClickable(!showBack);
            cardBack.setClickable(showBack);
            btnFrontSpeak.setClickable(!showBack);
            btnBackSpeak.setClickable(showBack);
            btnFrontFullscreen.setClickable(!showBack);
            btnBackFullscreen.setClickable(showBack);
        }

        /**
         * Continuous 180-degree horizontal-axis flip (rotationX) using hardware layer + alpha swap
         * so zero layout passes occur mid-animation.
         */
        private void animateContinuousFlip(float fromAngle, float toAngle) {
            flipContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            activeAnimator = ValueAnimator.ofFloat(fromAngle, toAngle);
            activeAnimator.setDuration(380L);
            activeAnimator.setInterpolator(new FastOutSlowInInterpolator());
            activeAnimator.addUpdateListener(anim -> {
                float angle = (float) anim.getAnimatedValue();
                flipContainer.setRotationX(angle);
                float mod = ((angle % 360f) + 360f) % 360f;
                applyFaceVisibility(mod >= 90f && mod <= 270f);
            });
            activeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    flipContainer.setRotationX(toAngle);
                    flipContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    flipContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            });
            activeAnimator.start();
        }
    }
}
