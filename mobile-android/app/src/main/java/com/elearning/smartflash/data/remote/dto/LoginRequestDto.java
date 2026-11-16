package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LoginRequestDto {

    @SerializedName("identifier")
    private final String identifier;

    @SerializedName("password")
    private final String password;

    @SerializedName("platform")
    private final String platform;

    @SerializedName("deviceInfo")
    private final String deviceInfo;

    public LoginRequestDto(String identifier, String password, String deviceInfo) {
        this.identifier = identifier;
        this.password = password;
        this.platform = "ANDROID";
        this.deviceInfo = deviceInfo;
    }
}
