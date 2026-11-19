package com.elearning.smartflash.ui.set;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.FlashcardItemDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter displaying the terms list inside {@link SetDetailActivity}
 * with per-card TTS pronunciation and star toggling.
 */
public class SetTermAdapter extends RecyclerView.Adapter<SetTermAdapter.TermViewHolder> {

    public interface OnTermActionListener {
        void onSpeakTerm(FlashcardItemDto card);
        void onSpeakDefinition(FlashcardItemDto card);
        void onToggleStar(FlashcardItemDto card);
    }

    private final OnTermActionListener listener;
    private final List<FlashcardItemDto> items = new ArrayList<>();
    private Set<Long> starredIds = new HashSet<>();

    public SetTermAdapter(OnTermActionListener listener) {
        this.listener = listener;
    }

    public void submitData(List<FlashcardItemDto> cards, Set<Long> starredIds) {
        this.items.clear();
        if (cards != null) {
            this.items.addAll(cards);
        }
        this.starredIds = starredIds != null ? new HashSet<>(starredIds) : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TermViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_set_detail_term, parent, false);
        return new TermViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TermViewHolder holder, int position) {
        FlashcardItemDto card = items.get(position);
        boolean isStarred = card.getId() != null && starredIds.contains(card.getId());
        holder.bind(card, isStarred, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TermViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTerm;
        private final TextView tvDefinition;
        private final TextView tvHint;
        private final ImageView ivThumb;
        private final ImageButton btnSpeak;
        private final ImageButton btnStar;

        TermViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTerm = itemView.findViewById(R.id.tv_term_item_word);
            tvDefinition = itemView.findViewById(R.id.tv_term_item_definition);
            tvHint = itemView.findViewById(R.id.tv_term_item_hint);
            ivThumb = itemView.findViewById(R.id.iv_term_item_image);
            btnSpeak = itemView.findViewById(R.id.btn_term_item_speak);
            btnStar = itemView.findViewById(R.id.btn_term_item_star);
        }

        void bind(FlashcardItemDto card, boolean isStarred, OnTermActionListener listener) {
            tvTerm.setText(!TextUtils.isEmpty(card.getTerm()) ? card.getTerm() : "");
            tvDefinition.setText(!TextUtils.isEmpty(card.getDefinition()) ? card.getDefinition() : "");

            if (!TextUtils.isEmpty(card.getHint())) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText("Gợi ý: " + card.getHint());
            } else {
                tvHint.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(card.getImageUrl())) {
                ivThumb.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(card.getImageUrl())
                        .centerCrop()
                        .into(ivThumb);
            } else {
                ivThumb.setVisibility(View.GONE);
            }

            btnStar.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            btnStar.setColorFilter(ContextCompat.getColor(
                    itemView.getContext(),
                    isStarred ? R.color.quizlet_yellow : R.color.sf_text_secondary
            ));

            btnSpeak.setOnClickListener(v -> {
                if (listener != null) listener.onSpeakTerm(card);
            });
            btnSpeak.setOnLongClickListener(v -> {
                if (listener != null) listener.onSpeakDefinition(card);
                return true;
            });
            tvDefinition.setOnClickListener(v -> {
                if (listener != null) listener.onSpeakDefinition(card);
            });
            btnStar.setOnClickListener(v -> {
                if (listener != null) listener.onToggleStar(card);
            });
        }
    }
}
