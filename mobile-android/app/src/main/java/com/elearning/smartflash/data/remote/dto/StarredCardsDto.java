package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response matching Spring Boot {@code StarredCardsResponse}.
 */
public class StarredCardsDto {

    @SerializedName("setId")
    private Long setId;

    @SerializedName("starredCardIds")
    private List<Long> starredCardIds;

    public Long getSetId() {
        return setId;
    }

    public List<Long> getStarredCardIds() {
        return starredCardIds;
    }
}
