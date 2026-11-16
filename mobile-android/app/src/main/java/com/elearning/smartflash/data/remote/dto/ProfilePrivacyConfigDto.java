package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Granular tab-level visibility settings matching backend {@code ProfilePrivacyConfig}.
 */
public class ProfilePrivacyConfigDto {

    @SerializedName("showSets")
    private boolean showSets = true;

    @SerializedName("showFolders")
    private boolean showFolders = true;

    @SerializedName("showClasses")
    private boolean showClasses = true;

    @SerializedName("showAchievements")
    private boolean showAchievements = true;

    public ProfilePrivacyConfigDto() {
    }

    public ProfilePrivacyConfigDto(
            boolean showSets,
            boolean showFolders,
            boolean showClasses,
            boolean showAchievements
    ) {
        this.showSets = showSets;
        this.showFolders = showFolders;
        this.showClasses = showClasses;
        this.showAchievements = showAchievements;
    }

    public boolean isShowSets() {
        return showSets;
    }

    public void setShowSets(boolean showSets) {
        this.showSets = showSets;
    }

    public boolean isShowFolders() {
        return showFolders;
    }

    public void setShowFolders(boolean showFolders) {
        this.showFolders = showFolders;
    }

    public boolean isShowClasses() {
        return showClasses;
    }

    public void setShowClasses(boolean showClasses) {
        this.showClasses = showClasses;
    }

    public boolean isShowAchievements() {
        return showAchievements;
    }

    public void setShowAchievements(boolean showAchievements) {
        this.showAchievements = showAchievements;
    }
}
