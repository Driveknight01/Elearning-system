package com.elearning.smartflash.ui.library;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.FolderSummaryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Library "Thư mục" tab.
 */
public class LibraryFolderAdapter extends RecyclerView.Adapter<LibraryFolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderSummaryDto folder);
    }

    private final List<FolderSummaryDto> folders = new ArrayList<>();
    private final OnFolderClickListener listener;

    public LibraryFolderAdapter(OnFolderClickListener listener) {
        this.listener = listener;
    }

    public void submitFolders(List<FolderSummaryDto> newFolders) {
        folders.clear();
        if (newFolders != null) {
            folders.addAll(newFolders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderSummaryDto folder = folders.get(position);
        String name = !TextUtils.isEmpty(folder.getName()) ? folder.getName() : "Thư mục không tên";
        holder.tvTitle.setText(name);

        long count = folder.getSetCount();
        holder.tvMeta.setText(count > 0 ? "Thư mục  •  " + count + " học phần" : "Thư mục");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderClick(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMeta;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_library_folder_title);
            tvMeta = itemView.findViewById(R.id.tv_library_folder_meta);
        }
    }
}
