package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PUT /api/v1/users/me/password}.
 */
public class ChangePasswordRequestDto {

    @SerializedName("currentPassword")
    private final String currentPassword;

    @SerializedName("newPassword")
    private final String newPassword;

    public ChangePasswordRequestDto(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
