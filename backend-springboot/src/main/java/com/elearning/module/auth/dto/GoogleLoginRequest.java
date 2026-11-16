package com.elearning.module.auth.dto;

import com.elearning.module.auth.entity.PlatformType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified Google Sign-In request payload shared by Web React and Android Native.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID Token không được để trống")
    private String idToken;

    private PlatformType platform;
    private String deviceInfo;
    private String fcmToken;
}
