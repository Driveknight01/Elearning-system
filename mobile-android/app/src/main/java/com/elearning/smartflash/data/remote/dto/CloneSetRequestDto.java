package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Optional request body when cloning a flashcard set.
 */
public class CloneSetRequestDto {

    @SerializedName("title")
    private final String title;

    @SerializedName("visibility")
    private final String visibility;

    public CloneSetRequestDto(String title, String visibility) {
        this.title = title;
        this.visibility = visibility;
    }

    public String getTitle() {
        return title;
    }

    public String getVisibility() {
        return visibility;
    }
}
