package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponseDto {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("tokenType")
    private String tokenType;

    @SerializedName("expiresInMs")
    private long expiresInMs;

    @SerializedName("user")
    private UserSummaryDto user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public UserSummaryDto getUser() {
        return user;
    }
}
