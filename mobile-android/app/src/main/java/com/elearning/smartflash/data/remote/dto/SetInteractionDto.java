package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response matching Spring Boot {@code SetInteractionResponse}.
 */
public class SetInteractionDto {

    @SerializedName("setId")
    private Long setId;

    @SerializedName("setTitle")
    private String setTitle;

    @SerializedName("saved")
    private boolean saved;

    @SerializedName("rating")
    private Integer rating;

    @SerializedName("reviewComment")
    private String reviewComment;

    @SerializedName("lastStudiedAt")
    private String lastStudiedAt;

    public Long getSetId() {
        return setId;
    }

    public String getSetTitle() {
        return setTitle;
    }

    public boolean isSaved() {
        return saved;
    }

    public Integer getRating() {
        return rating;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public String getLastStudiedAt() {
        return lastStudiedAt;
    }
}
