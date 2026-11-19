package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Lightweight user summary returned by {@code GET /api/v1/users/search}.
 */
public class UserSearchItemDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("role")
    private String role;

    @SerializedName("verifiedTeacher")
    private boolean verifiedTeacher;

    @SerializedName("setCount")
    private long setCount;

    @SerializedName("classCount")
    private long classCount;

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

    public String getRole() {
        return role;
    }

    public boolean isVerifiedTeacher() {
        return verifiedTeacher;
    }

    public long getSetCount() {
        return setCount;
    }

    public long getClassCount() {
        return classCount;
    }
}
