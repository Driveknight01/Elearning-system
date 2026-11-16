package com.elearning.module.user.dto;

import com.elearning.module.user.entity.ProfilePrivacyConfig;
import com.elearning.module.user.entity.ProfileVisibility;
import com.elearning.module.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Public/semi-public user profile payload filtered by {@link ProfileVisibility}
 * and tab-level {@link ProfilePrivacyConfig} rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {

    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private Role role;
    private boolean verifiedTeacher;
    private ProfileVisibility profileVisibility;
    private ProfilePrivacyConfig visibleTabs;
    private boolean owner;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long totalXp;
    private Instant joinedAt;

    /** Non-null only when the Sets tab is visible to the viewer. */
    private List<ProfileSetItem> sets;

    /** Non-null only when the Folders tab is visible to the viewer. */
    private List<ProfileFolderItem> folders;

    /** Non-null only when the Classes tab is visible to the viewer. */
    private List<ProfileClassItem> classes;

    /** Non-null only when the Achievements tab is visible to the viewer. */
    private List<ProfileAchievementItem> achievements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileSetItem {
        private Long id;
        private String title;
        private String description;
        private String visibility;
        private Integer cardCount;
        private Integer viewCount;
        private Double avgRating;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileFolderItem {
        private Long id;
        private String name;
        private String description;
        private String visibility;
        private Integer setCount;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileClassItem {
        private Long id;
        private String name;
        private String schoolName;
        private String memberRole;
        private Integer memberCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileAchievementItem {
        private Long id;
        private String code;
        private String name;
        private String description;
        private String iconUrl;
        private Integer xpReward;
        private Instant unlockedAt;
    }
}
