package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterRequestDto {

    @SerializedName("username")
    private final String username;

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("fullName")
    private final String fullName;

    @SerializedName("role")
    private final String role;

    @SerializedName("platform")
    private final String platform;

    @SerializedName("deviceInfo")
    private final String deviceInfo;

    public RegisterRequestDto(String username, String email, String password, String fullName, String role, String deviceInfo) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.platform = "ANDROID";
        this.deviceInfo = deviceInfo;
    }
}
