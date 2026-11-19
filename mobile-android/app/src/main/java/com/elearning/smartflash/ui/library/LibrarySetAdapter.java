package com.elearning.smartflash.ui.library;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Multi-type RecyclerView adapter for the Library "Học phần" tab,
 * grouping flashcard sets under time section headers ("Vài phút trước", "Hôm nay", "Trước đó").
 */
public class LibrarySetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSetClickListener {
        void onSetClick(SetSummaryDto set);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SET = 1;

    private static final class RowItem {
        final int type;
        final String headerTitle;
        final SetSummaryDto set;

        RowItem(String headerTitle) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.set = null;
        }

        RowItem(SetSummaryDto set) {
            this.type = TYPE_SET;
            this.headerTitle = null;
            this.set = set;
        }
    }

    private final List<RowItem> rows = new ArrayList<>();
    private final OnSetClickListener listener;
    private Long currentUserId;

    public LibrarySetAdapter(Long currentUserId, OnSetClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitSets(List<SetSummaryDto> sets) {
        rows.clear();
        if (sets != null && !sets.isEmpty()) {
            Map<String, List<SetSummaryDto>> grouped = new LinkedHashMap<>();
            long nowMs = System.currentTimeMillis();
            for (SetSummaryDto set : sets) {
                String bucket = resolveTimeBucket(set, nowMs);
                List<SetSummaryDto> list = grouped.get(bucket);
                if (list == null) {
                    list = new ArrayList<>();
                    grouped.put(bucket, list);
                }
                list.add(set);
            }
            for (Map.Entry<String, List<SetSummaryDto>> entry : grouped.entrySet()) {
                rows.add(new RowItem(entry.getKey()));
                for (SetSummaryDto item : entry.getValue()) {
                    rows.add(new RowItem(item));
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_library_section_header, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_library_set, parent, false);
        return new SetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowItem row = rows.get(position);
        if (holder instanceof HeaderViewHolder headerHolder) {
            headerHolder.tvHeader.setText(row.headerTitle);
        } else if (holder instanceof SetViewHolder setHolder && row.set != null) {
            SetSummaryDto set = row.set;
            String title = !TextUtils.isEmpty(set.getTitle()) ? set.getTitle() : "Chưa đặt tiêu đề";
            setHolder.tvTitle.setText(title);

            int termCount = set.getCardCount() != null ? set.getCardCount() : 0;
            String authorLabel = resolveAuthorLabel(set.getOwner());
            setHolder.tvMeta.setText("Học phần • " + termCount + " thuật ngữ • " + authorLabel);

            setHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSetClick(set);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private String resolveAuthorLabel(SetSummaryDto.OwnerSummaryDto owner) {
        if (owner == null) {
            return "bởi bạn";
        }
        if (currentUserId != null && owner.getId() != null && currentUserId.equals(owner.getId())) {
            return "bởi bạn";
        }
        String name = !TextUtils.isEmpty(owner.getUsername()) ? owner.getUsername() : owner.getFullName();
        return !TextUtils.isEmpty(name) ? "bởi " + name : "bởi bạn";
    }

    private static String resolveTimeBucket(SetSummaryDto set, long nowMs) {
        String iso = !TextUtils.isEmpty(set.getUpdatedAt()) ? set.getUpdatedAt() : set.getCreatedAt();
        Date parsed = parseIsoDate(iso);
        if (parsed == null) {
            return "Gần đây";
        }
        long diffMs = Math.max(0L, nowMs - parsed.getTime());
        if (diffMs < 60L * 60L * 1000L) {
            return "Vài phút trước";
        }
        Calendar calNow = Calendar.getInstance();
        calNow.setTimeInMillis(nowMs);
        Calendar calItem = Calendar.getInstance();
        calItem.setTime(parsed);

        if (calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR)
                && calNow.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)) {
            return "Hôm nay";
        }
        calNow.add(Calendar.DAY_OF_YEAR, -1);
        if (calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR)
                && calNow.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)) {
            return "Hôm qua";
        }
        return "Trước đó";
    }

    private static Date parseIsoDate(String iso) {
        if (TextUtils.isEmpty(iso)) {
            return null;
        }
        String normalized = iso.trim();
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int dotIdx = normalized.indexOf('.');
        if (dotIdx > 0) {
            normalized = normalized.substring(0, dotIdx);
        }
        int plusIdx = normalized.indexOf('+');
        if (plusIdx > 0) {
            normalized = normalized.substring(0, plusIdx);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(normalized);
        } catch (ParseException e) {
            return null;
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tv_section_header);
        }
    }

    static class SetViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMeta;

        SetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_library_set_title);
            tvMeta = itemView.findViewById(R.id.tv_library_set_meta);
        }
    }
}
