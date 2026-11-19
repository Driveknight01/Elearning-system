package com.elearning.smartflash.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.databinding.ItemStaffPickCardBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal card adapter for Newbie Home Block 4 ("Gợi ý chọn lọc / Staff picks").
 */
public class StaffPickAdapter extends RecyclerView.Adapter<StaffPickAdapter.PickViewHolder> {

    public static class StaffPickItem {
        public final long id;
        public final String title;
        public final int termCount;
        public final String author;

        public StaffPickItem(long id, String title, int termCount, String author) {
            this.id = id;
            this.title = title;
            this.termCount = termCount;
            this.author = author;
        }
    }

    public interface OnItemClickListener {
        void onPickClick(StaffPickItem item);
    }

    private final List<StaffPickItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public StaffPickAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<StaffPickItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffPickCardBinding binding = ItemStaffPickCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PickViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PickViewHolder holder, int position) {
        StaffPickItem item = items.get(position);
        holder.binding.tvPickTitle.setText(item.title);
        holder.binding.tvPickMeta.setText(item.termCount + " thẻ • bởi " + item.author);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onPickClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PickViewHolder extends RecyclerView.ViewHolder {
        final ItemStaffPickCardBinding binding;

        PickViewHolder(ItemStaffPickCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
