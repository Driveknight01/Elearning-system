package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Detailed folder response matching Spring Boot {@code FolderDetailResponse}.
 */
public class FolderDetailDto {

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

    @SerializedName("ownerView")
    private boolean ownerView;

    @SerializedName("sets")
    private List<SetSummaryDto> sets;

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

    public boolean isOwnerView() {
        return ownerView;
    }

    public List<SetSummaryDto> getSets() {
        return sets;
    }
}
