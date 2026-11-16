package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PUT /api/v1/users/me/privacy}.
 */
public class UpdatePrivacyRequestDto {

    @SerializedName("profileVisibility")
    private final String profileVisibility;

    @SerializedName("profilePrivacyConfig")
    private final ProfilePrivacyConfigDto profilePrivacyConfig;

    public UpdatePrivacyRequestDto(
            String profileVisibility,
            ProfilePrivacyConfigDto profilePrivacyConfig
    ) {
        this.profileVisibility = profileVisibility;
        this.profilePrivacyConfig = profilePrivacyConfig;
    }
}
