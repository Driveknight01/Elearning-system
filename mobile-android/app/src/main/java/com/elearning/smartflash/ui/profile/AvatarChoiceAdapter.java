package com.elearning.smartflash.ui.profile;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ItemAvatarChoiceBinding;

import java.util.List;

/**
 * 3-column grid adapter for Quizlet-style avatar picker (Camera, Gallery, and 12 Web presets).
 */
public class AvatarChoiceAdapter extends RecyclerView.Adapter<AvatarChoiceAdapter.TileViewHolder> {

    public static final class AvatarTile {
        public static final int TYPE_CAMERA = 0;
        public static final int TYPE_GALLERY = 1;
        public static final int TYPE_PRESET = 2;

        public final int type;
        @DrawableRes
        public final int iconRes;
        public final String id;
        public final String bgHex;
        public final String emoji;

        public static AvatarTile action(int type, @DrawableRes int iconRes) {
            return new AvatarTile(type, iconRes, null, "#F3F4F8", null);
        }

        public static AvatarTile preset(String id, String bgHex, String emoji) {
            return new AvatarTile(TYPE_PRESET, 0, id, bgHex, emoji);
        }

        private AvatarTile(int type, int iconRes, String id, String bgHex, String emoji) {
            this.type = type;
            this.iconRes = iconRes;
            this.id = id;
            this.bgHex = bgHex;
            this.emoji = emoji;
        }
    }

    public interface OnTileClickListener {
        void onTileClicked(AvatarTile tile, int position);
    }

    private final List<AvatarTile> tiles;
    private final OnTileClickListener listener;
    private int selectedPosition = -1;

    public AvatarChoiceAdapter(List<AvatarTile> tiles, OnTileClickListener listener) {
        this.tiles = tiles;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int prev = selectedPosition;
        selectedPosition = position;
        if (prev >= 0) notifyItemChanged(prev);
        if (selectedPosition >= 0) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAvatarChoiceBinding binding = ItemAvatarChoiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        AvatarTile tile = tiles.get(position);
        boolean isSelected = (position == selectedPosition);

        holder.binding.cardAvatarTile.setStrokeColor(isSelected
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.quizlet_blue)
                : Color.TRANSPARENT);
        holder.binding.flTileSelectedBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        if (tile.type == AvatarTile.TYPE_PRESET) {
            holder.binding.cardAvatarTile.setCardBackgroundColor(Color.parseColor(tile.bgHex));
            holder.binding.ivTileIcon.setVisibility(View.GONE);
            holder.binding.tvTileEmoji.setVisibility(View.VISIBLE);
            holder.binding.tvTileEmoji.setText(tile.emoji);
        } else {
            holder.binding.cardAvatarTile.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_bg_surface_alt));
            holder.binding.tvTileEmoji.setVisibility(View.GONE);
            holder.binding.ivTileIcon.setVisibility(View.VISIBLE);
            holder.binding.ivTileIcon.setImageResource(tile.iconRes);
            holder.binding.ivTileIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.sf_text_primary));
        }

        holder.binding.cardAvatarTile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTileClicked(tile, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return tiles.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        final ItemAvatarChoiceBinding binding;

        TileViewHolder(ItemAvatarChoiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
