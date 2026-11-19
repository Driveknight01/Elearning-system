package com.elearning.smartflash.ui.library;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.StudyGroupSummaryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Library "Lớp học" tab.
 */
public class LibraryClassAdapter extends RecyclerView.Adapter<LibraryClassAdapter.ClassViewHolder> {

    public interface OnClassClickListener {
        void onClassClick(StudyGroupSummaryDto group);
    }

    private final List<StudyGroupSummaryDto> groups = new ArrayList<>();
    private final OnClassClickListener listener;

    public LibraryClassAdapter(OnClassClickListener listener) {
        this.listener = listener;
    }

    public void submitGroups(List<StudyGroupSummaryDto> newGroups) {
        groups.clear();
        if (newGroups != null) {
            groups.addAll(newGroups);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        StudyGroupSummaryDto group = groups.get(position);
        String name = !TextUtils.isEmpty(group.getName()) ? group.getName() : "Lớp học";
        holder.tvTitle.setText(name);
        holder.tvMeta.setText("Lớp học • " + group.getSetCount() + " học phần • " + group.getMemberCount() + " thành viên");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClassClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMeta;

        ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_library_class_title);
            tvMeta = itemView.findViewById(R.id.tv_library_class_meta);
        }
    }
}
