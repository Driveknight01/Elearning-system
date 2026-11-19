package com.elearning.smartflash.ui.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.databinding.ItemSearchClassCardBinding;
import com.elearning.smartflash.ui.search.SearchResultModels.ClassItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Class search result cards.
 */
public class SearchClassAdapter extends RecyclerView.Adapter<SearchClassAdapter.ClassViewHolder> {

    public interface OnClassClickListener {
        void onClassClick(ClassItem item);
    }

    private final List<ClassItem> items = new ArrayList<>();
    private final OnClassClickListener listener;

    public SearchClassAdapter(OnClassClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ClassItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchClassCardBinding binding = ItemSearchClassCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ClassViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassItem item = items.get(position);
        holder.binding.tvClassTitle.setText(item.title);
        holder.binding.tvClassSchool.setText(item.schoolInfo);
        holder.binding.tvClassSetsCount.setText(item.setCount + " học phần");
        holder.binding.tvClassMembersCount.setText(item.memberCount + " thành viên");
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onClassClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchClassCardBinding binding;

        ClassViewHolder(ItemSearchClassCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
