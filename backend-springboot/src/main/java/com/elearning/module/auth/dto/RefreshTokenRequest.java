package com.elearning.module.auth.dto;

import com.elearning.module.auth.entity.PlatformType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;

    private PlatformType platform;
    private String deviceInfo;
    private String fcmToken;
}
