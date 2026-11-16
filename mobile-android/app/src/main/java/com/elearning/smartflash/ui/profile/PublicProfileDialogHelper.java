package com.elearning.smartflash.ui.profile;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.ProfilePrivacyConfigDto;
import com.elearning.smartflash.data.remote.dto.PublicProfileDto;
import com.elearning.smartflash.util.ProfileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/**
 * Shared dialog presenter for rendering {@link PublicProfileDto} across 4 privacy-filtered tabs.
 */
public final class PublicProfileDialogHelper {

    private PublicProfileDialogHelper() {
    }

    public static void show(Context context, PublicProfileDto profile) {
        if (context == null || profile == null) {
            return;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_public_profile, null);
        TextView tvInitials = view.findViewById(R.id.tv_pub_initials);
        ImageView ivAvatar = view.findViewById(R.id.iv_pub_avatar);
        TextView tvFullName = view.findViewById(R.id.tv_pub_full_name);
        TextView tvMeta = view.findViewById(R.id.tv_pub_meta);
        TextView tvStats = view.findViewById(R.id.tv_pub_stats);
        TabLayout tabLayout = view.findViewById(R.id.tab_public_sections);
        TextView tvContent = view.findViewById(R.id.tv_pub_tab_content);

        String displayName = !TextUtils.isEmpty(profile.getFullName())
                ? profile.getFullName()
                : profile.getUsername();
        tvFullName.setText(displayName);
        tvMeta.setText("@" + profile.getUsername()
                + " • " + ProfileUtils.getRoleLabel(profile.getRole())
                + " • " + ProfileUtils.getVisibilityLabel(profile.getProfileVisibility()));

        int streak = profile.getCurrentStreak() != null ? profile.getCurrentStreak() : 0;
        long xp = profile.getTotalXp() != null ? profile.getTotalXp() : 0L;
        tvStats.setText("Chuỗi " + streak + " ngày • " + xp + " XP");

        bindAvatar(context, profile.getAvatarUrl(), displayName, profile.getUsername(), ivAvatar, tvInitials);

        renderTabContent(tvContent, profile, 0);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                renderTabContent(tvContent, profile, tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                renderTabContent(tvContent, profile, tab.getPosition());
            }
        });

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.profile_public_preview_title)
                .setView(view)
                .setPositiveButton("Đóng", (d, which) -> d.dismiss())
                .show();
    }

    public static void bindAvatar(
            Context context,
            String rawAvatarUrl,
            String fullName,
            String username,
            ImageView ivAvatar,
            TextView tvInitials
    ) {
        tvInitials.setText(ProfileUtils.getInitials(fullName, username));
        String resolvedUrl = ProfileUtils.resolveImageUrl(rawAvatarUrl);
        if (!TextUtils.isEmpty(resolvedUrl)) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvInitials.setVisibility(View.INVISIBLE);
            Glide.with(context)
                    .load(resolvedUrl)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvInitials.setVisibility(View.VISIBLE);
        }
    }

    private static void renderTabContent(TextView tvContent, PublicProfileDto profile, int position) {
        ProfilePrivacyConfigDto visible = profile.getVisibleTabs();
        switch (position) {
            case 0:
                if (visible != null && !visible.isShowSets()) {
                    tvContent.setText("Chủ tài khoản đã ẩn tab Bộ thẻ.");
                    return;
                }
                tvContent.setText(formatSets(profile.getSets()));
                break;
            case 1:
                if (visible != null && !visible.isShowFolders()) {
                    tvContent.setText("Chủ tài khoản đã ẩn tab Thư mục.");
                    return;
                }
                tvContent.setText(formatFolders(profile.getFolders()));
                break;
            case 2:
                if (visible != null && !visible.isShowClasses()) {
                    tvContent.setText("Chủ tài khoản đã ẩn tab Lớp học.");
                    return;
                }
                tvContent.setText(formatClasses(profile.getClasses()));
                break;
            case 3:
                if (visible != null && !visible.isShowAchievements()) {
                    tvContent.setText("Chủ tài khoản đã ẩn tab Thành tựu.");
                    return;
                }
                tvContent.setText(formatAchievements(profile.getAchievements()));
                break;
            default:
                break;
        }
    }

    private static String formatSets(List<PublicProfileDto.ProfileSetItemDto> items) {
        if (items == null || items.isEmpty()) {
            return "Chưa có bộ thẻ công khai nào.";
        }
        StringBuilder sb = new StringBuilder();
        for (PublicProfileDto.ProfileSetItemDto item : items) {
            sb.append("• ").append(item.getTitle())
                    .append(" (").append(item.getCardCount() != null ? item.getCardCount() : 0).append(" thẻ)\n");
        }
        return sb.toString().trim();
    }

    private static String formatFolders(List<PublicProfileDto.ProfileFolderItemDto> items) {
        if (items == null || items.isEmpty()) {
            return "Chưa có thư mục công khai nào.";
        }
        StringBuilder sb = new StringBuilder();
        for (PublicProfileDto.ProfileFolderItemDto item : items) {
            sb.append("• ").append(item.getName())
                    .append(" (").append(item.getSetCount() != null ? item.getSetCount() : 0).append(" bộ thẻ)\n");
        }
        return sb.toString().trim();
    }

    private static String formatClasses(List<PublicProfileDto.ProfileClassItemDto> items) {
        if (items == null || items.isEmpty()) {
            return "Chưa tham gia hoặc giảng dạy lớp học nào.";
        }
        StringBuilder sb = new StringBuilder();
        for (PublicProfileDto.ProfileClassItemDto item : items) {
            sb.append("• ").append(item.getName());
            if (!TextUtils.isEmpty(item.getSchoolName())) {
                sb.append(" — ").append(item.getSchoolName());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String formatAchievements(List<PublicProfileDto.ProfileAchievementItemDto> items) {
        if (items == null || items.isEmpty()) {
            return "Chưa mở khóa huy hiệu thành tựu nào.";
        }
        StringBuilder sb = new StringBuilder();
        for (PublicProfileDto.ProfileAchievementItemDto item : items) {
            sb.append("• ").append(item.getName())
                    .append(" (+").append(item.getXpReward() != null ? item.getXpReward() : 0).append(" XP)\n");
        }
        return sb.toString().trim();
    }
}
