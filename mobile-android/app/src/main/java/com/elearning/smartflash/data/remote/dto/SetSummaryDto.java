package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Summary response matching Spring Boot {@code SetSummaryResponse}.
 */
public class SetSummaryDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("featured")
    private boolean featured;

    @SerializedName("shareToken")
    private String shareToken;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("cardCount")
    private Integer cardCount;

    @SerializedName("viewCount")
    private Integer viewCount;

    @SerializedName("cloneCount")
    private Integer cloneCount;

    @SerializedName("owner")
    private OwnerSummaryDto owner;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

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

    public boolean isFeatured() {
        return featured;
    }

    public String getShareToken() {
        return shareToken;
    }

    public List<String> getTags() {
        return tags;
    }

    public Integer getCardCount() {
        return cardCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public Integer getCloneCount() {
        return cloneCount;
    }

    public OwnerSummaryDto getOwner() {
        return owner;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public static class OwnerSummaryDto {

        @SerializedName("id")
        private Long id;

        @SerializedName("username")
        private String username;

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("avatarUrl")
        private String avatarUrl;

        @SerializedName("verifiedTeacher")
        private boolean verifiedTeacher;

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

        public boolean isVerifiedTeacher() {
            return verifiedTeacher;
        }
    }
}
