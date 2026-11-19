package com.elearning.smartflash.ui.search;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ItemSearchSuggestionBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Autocomplete suggestion row adapter matching Quizlet's prefix-normal / suffix-bold styling.
 */
public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    private final List<String> suggestions = new ArrayList<>();
    private String currentQuery = "";
    private final OnSuggestionClickListener listener;

    public SearchSuggestionAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void submitSuggestions(String query, List<String> items) {
        this.currentQuery = query != null ? query.trim() : "";
        suggestions.clear();
        if (items != null) {
            suggestions.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchSuggestionBinding binding = ItemSearchSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SuggestionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String text = suggestions.get(position);
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQuery = currentQuery.toLowerCase(Locale.ROOT);

        int primaryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_text_primary);
        int secondaryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_text_secondary);

        if (!lowerQuery.isEmpty() && lowerText.startsWith(lowerQuery) && text.length() > currentQuery.length()) {
            SpannableString span = new SpannableString(text);
            span.setSpan(new ForegroundColorSpan(secondaryColor), 0, currentQuery.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(Typeface.BOLD), currentQuery.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(primaryColor), currentQuery.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.binding.tvSuggestionText.setText(span);
        } else {
            holder.binding.tvSuggestionText.setTextColor(secondaryColor);
            holder.binding.tvSuggestionText.setText(text);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick(text);
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchSuggestionBinding binding;

        SuggestionViewHolder(ItemSearchSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
