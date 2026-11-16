package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response payload for {@code POST /api/v1/users/me/avatar}.
 */
public class AvatarUploadResponseDto {

    @SerializedName("avatarUrl")
    private String avatarUrl;

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
