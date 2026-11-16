package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Authenticated user's full profile returned by {@code GET /api/v1/users/me}.
 */
public class MyProfileDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

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

    @SerializedName("status")
    private String status;

    @SerializedName("dailyGoalCards")
    private Integer dailyGoalCards;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("profileVisibility")
    private String profileVisibility;

    @SerializedName("profilePrivacyConfig")
    private ProfilePrivacyConfigDto profilePrivacyConfig;

    @SerializedName("hasPassword")
    private boolean hasPassword;

    @SerializedName("currentStreak")
    private Integer currentStreak;

    @SerializedName("longestStreak")
    private Integer longestStreak;

    @SerializedName("totalXp")
    private Long totalXp;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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

    public String getStatus() {
        return status;
    }

    public Integer getDailyGoalCards() {
        return dailyGoalCards;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public ProfilePrivacyConfigDto getProfilePrivacyConfig() {
        return profilePrivacyConfig;
    }

    public boolean isHasPassword() {
        return hasPassword;
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

    public String getCreatedAt() {
        return createdAt;
    }
}
