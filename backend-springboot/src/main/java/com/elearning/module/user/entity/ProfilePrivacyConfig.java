package com.elearning.module.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Granular tab-level visibility settings stored in {@code users.profile_privacy_config} JSONB column.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePrivacyConfig implements Serializable {

    @Builder.Default
    private boolean showSets = true;

    @Builder.Default
    private boolean showFolders = true;

    @Builder.Default
    private boolean showClasses = true;

    @Builder.Default
    private boolean showAchievements = true;
}
