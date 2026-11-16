package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class GoogleLoginRequestDto {

    @SerializedName("idToken")
    private final String idToken;

    @SerializedName("platform")
    private final String platform;

    @SerializedName("deviceInfo")
    private final String deviceInfo;

    public GoogleLoginRequestDto(String idToken, String deviceInfo) {
        this.idToken = idToken;
        this.platform = "ANDROID";
        this.deviceInfo = deviceInfo;
    }
}
