package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UserSummaryDto {

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

    @SerializedName("role")
    private String role;

    @SerializedName("verifiedTeacher")
    private boolean verifiedTeacher;

    @SerializedName("status")
    private String status;

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

    public String getRole() {
        return role;
    }

    public boolean isVerifiedTeacher() {
        return verifiedTeacher;
    }

    public String getStatus() {
        return status;
    }
}
