package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordRequestDto {

    @SerializedName("email")
    private final String email;

    public ForgotPasswordRequestDto(String email) {
        this.email = email;
    }
}
