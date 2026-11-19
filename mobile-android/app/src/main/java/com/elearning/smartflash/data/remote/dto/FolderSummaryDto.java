package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Summary response matching Spring Boot {@code FolderSummaryResponse}.
 */
public class FolderSummaryDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("shareToken")
    private String shareToken;

    @SerializedName("setCount")
    private long setCount;

    public Long getId() {
        return id;
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

    public String getShareToken() {
        return shareToken;
    }

    public long getSetCount() {
        return setCount;
    }
}
