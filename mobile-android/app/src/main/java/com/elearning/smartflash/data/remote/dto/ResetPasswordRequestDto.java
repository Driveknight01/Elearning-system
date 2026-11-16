package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequestDto {

    @SerializedName("email")
    private final String email;

    @SerializedName("otpCode")
    private final String otpCode;

    @SerializedName("newPassword")
    private final String newPassword;

    public ResetPasswordRequestDto(String email, String otpCode, String newPassword) {
        this.email = email;
        this.otpCode = otpCode;
        this.newPassword = newPassword;
    }
}
