package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for rating a flashcard set (1-5 stars).
 */
public class RateSetRequestDto {

    @SerializedName("rating")
    private final int rating;

    @SerializedName("comment")
    private final String comment;

    public RateSetRequestDto(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
