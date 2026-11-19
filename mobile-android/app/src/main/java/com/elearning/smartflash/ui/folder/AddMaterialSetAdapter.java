package com.elearning.smartflash.ui.folder;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;
import com.elearning.smartflash.databinding.ItemAddMaterialSetBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Selectable flashcard set adapter for the "Thêm tài liệu học" modal (Images 2, 4, 5).
 */
public class AddMaterialSetAdapter extends RecyclerView.Adapter<AddMaterialSetAdapter.MaterialViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<SetSummaryDto> allSets = new ArrayList<>();
    private final List<SetSummaryDto> visibleSets = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();
    private final OnSelectionChangedListener listener;

    public AddMaterialSetAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void submitSets(List<SetSummaryDto> sets) {
        allSets.clear();
        if (sets != null) {
            allSets.addAll(sets);
        }
        filter("");
    }

    public void filter(String query) {
        visibleSets.clear();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        for (SetSummaryDto s : allSets) {
            if (q.isEmpty()) {
                visibleSets.add(s);
            } else {
                String title = s.getTitle() != null ? s.getTitle().toLowerCase(Locale.ROOT) : "";
                if (title.contains(q)) {
                    visibleSets.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddMaterialSetBinding binding = ItemAddMaterialSetBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MaterialViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        SetSummaryDto set = visibleSets.get(position);
        Long id = set.getId();
        boolean isSelected = id != null && selectedIds.contains(id);

        String title = !TextUtils.isEmpty(set.getTitle()) ? set.getTitle() : "Chưa đặt tiêu đề";
        int termCount = set.getCardCount() != null ? set.getCardCount() : 0;
        String author = "bạn";
        if (set.getOwner() != null && !TextUtils.isEmpty(set.getOwner().getUsername())) {
            author = set.getOwner().getUsername();
        }

        holder.binding.tvMatTitle.setText(title);
        holder.binding.tvMatMeta.setText("Học phần • " + termCount + " thuật ngữ • bởi " + author);

        if (isSelected) {
            holder.binding.flMatToggle.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_text_primary))
            );
            holder.binding.ivMatToggle.setImageResource(R.drawable.ic_check);
            holder.binding.ivMatToggle.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_bg_screen)
            );
        } else {
            holder.binding.flMatToggle.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_bg_surface_alt))
            );
            holder.binding.ivMatToggle.setImageResource(R.drawable.ic_nav_add);
            holder.binding.ivMatToggle.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_text_secondary)
            );
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            if (id == null) return;
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
            } else {
                selectedIds.add(id);
            }
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null) {
                listener.onSelectionChanged(selectedIds.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleSets.size();
    }

    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        final ItemAddMaterialSetBinding binding;

        MaterialViewHolder(ItemAddMaterialSetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
