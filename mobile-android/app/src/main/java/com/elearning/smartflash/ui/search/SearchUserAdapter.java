package com.elearning.smartflash.ui.search;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.databinding.ItemSearchUserCardBinding;
import com.elearning.smartflash.ui.search.SearchResultModels.UserItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for User search result cards.
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserItem item);
    }

    private static final String[] PRESET_EMOJIS = {
            "\uD83E\uDD8A", "\uD83D\uDC3C", "\uD83E\uDD81", "\uD83D\uDC38",
            "\uD83D\uDC19", "\uD83E\uDD84", "\uD83D\uDC2F", "\uD83D\uDC28"
    };
    private static final String[] PRESET_BG_HEXES = {
            "#FDE68A", "#DBEAFE", "#FCE7F3", "#DCFCE7",
            "#EDE9FE", "#FFEDD5", "#E0F2FE", "#F3E8FF"
    };

    private final List<UserItem> items = new ArrayList<>();
    private final OnUserClickListener listener;

    public SearchUserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<UserItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchUserCardBinding binding = ItemSearchUserCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserItem item = items.get(position);
        holder.binding.tvUserName.setText(item.username);
        holder.binding.tvUserMeta.setText(item.setCount + " học phần • " + item.classCount + " lớp học");

        String emoji = PRESET_EMOJIS[position % PRESET_EMOJIS.length];
        String bgHex = PRESET_BG_HEXES[position % PRESET_BG_HEXES.length];
        holder.binding.tvUserAvatar.setText(emoji);
        holder.binding.tvUserAvatar.setTextSize(22f);
        holder.binding.tvUserAvatar.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor(bgHex))
        );

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchUserCardBinding binding;

        UserViewHolder(ItemSearchUserCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
