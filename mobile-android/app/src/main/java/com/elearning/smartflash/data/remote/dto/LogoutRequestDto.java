package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LogoutRequestDto {

    @SerializedName("refreshToken")
    private final String refreshToken;

    public LogoutRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
