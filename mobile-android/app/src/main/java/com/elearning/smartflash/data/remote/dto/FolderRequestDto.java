package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload matching Spring Boot {@code FolderRequest}.
 */
public class FolderRequestDto {

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("visibility")
    private String visibility;

    public FolderRequestDto(String name, String description, String visibility) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVisibility() {
        return visibility;
    }
}
