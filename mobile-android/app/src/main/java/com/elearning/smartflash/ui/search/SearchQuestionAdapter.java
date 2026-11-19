package com.elearning.smartflash.ui.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.databinding.ItemSearchQuestionCardBinding;
import com.elearning.smartflash.ui.search.SearchResultModels.QuestionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Question search result cards.
 */
public class SearchQuestionAdapter extends RecyclerView.Adapter<SearchQuestionAdapter.QuestionViewHolder> {

    public interface OnQuestionClickListener {
        void onQuestionClick(QuestionItem item);
    }

    private final List<QuestionItem> items = new ArrayList<>();
    private final OnQuestionClickListener listener;

    public SearchQuestionAdapter(OnQuestionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<QuestionItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchQuestionCardBinding binding = ItemSearchQuestionCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new QuestionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        QuestionItem item = items.get(position);
        holder.binding.tvQuestionSubject.setText(item.subject);
        holder.binding.tvQuestionBody.setText(item.body);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onQuestionClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchQuestionCardBinding binding;

        QuestionViewHolder(ItemSearchQuestionCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
