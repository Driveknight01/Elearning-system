package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenRequestDto {

    @SerializedName("refreshToken")
    private final String refreshToken;

    @SerializedName("platform")
    private final String platform;

    @SerializedName("deviceInfo")
    private final String deviceInfo;

    public RefreshTokenRequestDto(String refreshToken, String deviceInfo) {
        this.refreshToken = refreshToken;
        this.platform = "ANDROID";
        this.deviceInfo = deviceInfo;
    }
}
