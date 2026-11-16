package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Privacy-filtered user profile returned by {@code GET /api/v1/users/profile/{username}}.
 */
public class PublicProfileDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("bio")
    private String bio;

    @SerializedName("role")
    private String role;

    @SerializedName("verifiedTeacher")
    private boolean verifiedTeacher;

    @SerializedName("profileVisibility")
    private String profileVisibility;

    @SerializedName("visibleTabs")
    private ProfilePrivacyConfigDto visibleTabs;

    @SerializedName("owner")
    private boolean owner;

    @SerializedName("currentStreak")
    private Integer currentStreak;

    @SerializedName("longestStreak")
    private Integer longestStreak;

    @SerializedName("totalXp")
    private Long totalXp;

    @SerializedName("joinedAt")
    private String joinedAt;

    @SerializedName("sets")
    private List<ProfileSetItemDto> sets;

    @SerializedName("folders")
    private List<ProfileFolderItemDto> folders;

    @SerializedName("classes")
    private List<ProfileClassItemDto> classes;

    @SerializedName("achievements")
    private List<ProfileAchievementItemDto> achievements;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public String getRole() {
        return role;
    }

    public boolean isVerifiedTeacher() {
        return verifiedTeacher;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public ProfilePrivacyConfigDto getVisibleTabs() {
        return visibleTabs;
    }

    public boolean isOwner() {
        return owner;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public Integer getLongestStreak() {
        return longestStreak;
    }

    public Long getTotalXp() {
        return totalXp;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public List<ProfileSetItemDto> getSets() {
        return sets;
    }

    public List<ProfileFolderItemDto> getFolders() {
        return folders;
    }

    public List<ProfileClassItemDto> getClasses() {
        return classes;
    }

    public List<ProfileAchievementItemDto> getAchievements() {
        return achievements;
    }

    public static class ProfileSetItemDto {
        @SerializedName("id")
        private Long id;
        @SerializedName("title")
        private String title;
        @SerializedName("description")
        private String description;
        @SerializedName("visibility")
        private String visibility;
        @SerializedName("cardCount")
        private Integer cardCount;
        @SerializedName("viewCount")
        private Integer viewCount;
        @SerializedName("avgRating")
        private Double avgRating;
        @SerializedName("createdAt")
        private String createdAt;

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getVisibility() {
            return visibility;
        }

        public Integer getCardCount() {
            return cardCount;
        }

        public Integer getViewCount() {
            return viewCount;
        }

        public Double getAvgRating() {
            return avgRating;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static class ProfileFolderItemDto {
        @SerializedName("id")
        private Long id;
        @SerializedName("name")
        private String name;
        @SerializedName("description")
        private String description;
        @SerializedName("visibility")
        private String visibility;
        @SerializedName("setCount")
        private Integer setCount;
        @SerializedName("createdAt")
        private String createdAt;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getVisibility() {
            return visibility;
        }

        public Integer getSetCount() {
            return setCount;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static class ProfileClassItemDto {
        @SerializedName("id")
        private Long id;
        @SerializedName("name")
        private String name;
        @SerializedName("schoolName")
        private String schoolName;
        @SerializedName("memberRole")
        private String memberRole;
        @SerializedName("memberCount")
        private Integer memberCount;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSchoolName() {
            return schoolName;
        }

        public String getMemberRole() {
            return memberRole;
        }

        public Integer getMemberCount() {
            return memberCount;
        }
    }

    public static class ProfileAchievementItemDto {
        @SerializedName("id")
        private Long id;
        @SerializedName("code")
        private String code;
        @SerializedName("name")
        private String name;
        @SerializedName("description")
        private String description;
        @SerializedName("iconUrl")
        private String iconUrl;
        @SerializedName("xpReward")
        private Integer xpReward;
        @SerializedName("unlockedAt")
        private String unlockedAt;

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public Integer getXpReward() {
            return xpReward;
        }

        public String getUnlockedAt() {
            return unlockedAt;
        }
    }
}
