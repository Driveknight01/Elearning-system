package com.elearning.smartflash.ui.search;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.databinding.ItemSearchSetCardBinding;
import com.elearning.smartflash.ui.search.SearchResultModels.SetItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Flashcard Set search result cards (supports All Results & Sets tab modes).
 */
public class SearchSetAdapter extends RecyclerView.Adapter<SearchSetAdapter.SetViewHolder> {

    public interface OnSetActionListener {
        void onPreview(SetItem item);
        void onStudy(SetItem item);
    }

    private final List<SetItem> items = new ArrayList<>();
    private final boolean showPreviewButton;
    private final OnSetActionListener listener;

    public SearchSetAdapter(boolean showPreviewButton, OnSetActionListener listener) {
        this.showPreviewButton = showPreviewButton;
        this.listener = listener;
    }

    public void submitList(List<SetItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchSetCardBinding binding = ItemSearchSetCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        SetItem item = items.get(position);
        holder.binding.tvSearchSetTitle.setText(item.title);
        holder.binding.ivSearchSetHasImage.setVisibility(item.hasImage ? View.VISIBLE : View.GONE);
        holder.binding.tvSearchSetTerms.setText(item.termCount + " thuật ngữ");
        holder.binding.tvSearchSetAuthor.setText(item.author);

        String initial = item.author != null && !item.author.isEmpty()
                ? item.author.substring(0, 1).toUpperCase()
                : "U";
        holder.binding.tvSearchSetAvatar.setText(initial);
        holder.binding.tvSearchSetAvatar.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor(item.avatarHex))
        );

        holder.binding.btnSearchSetPreview.setVisibility(showPreviewButton ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams studyLp = (LinearLayout.LayoutParams) holder.binding.btnSearchSetStudy.getLayoutParams();
        if (showPreviewButton) {
            studyLp.width = 0;
            studyLp.weight = 1f;
        } else {
            float density = holder.itemView.getResources().getDisplayMetrics().density;
            studyLp.width = Math.round(140 * density);
            studyLp.weight = 0f;
        }
        holder.binding.btnSearchSetStudy.setLayoutParams(studyLp);
        holder.binding.btnSearchSetPreview.setOnClickListener(v -> {
            if (listener != null) listener.onPreview(item);
        });
        holder.binding.btnSearchSetStudy.setOnClickListener(v -> {
            if (listener != null) listener.onStudy(item);
        });
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onStudy(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SetViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchSetCardBinding binding;

        SetViewHolder(ItemSearchSetCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
