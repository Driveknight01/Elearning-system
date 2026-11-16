package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PUT /api/v1/users/me}.
 */
public class UpdateProfileRequestDto {

    @SerializedName("username")
    private final String username;

    @SerializedName("fullName")
    private final String fullName;

    @SerializedName("bio")
    private final String bio;

    @SerializedName("dailyGoalCards")
    private final Integer dailyGoalCards;

    @SerializedName("timezone")
    private final String timezone;

    public UpdateProfileRequestDto(
            String username,
            String fullName,
            String bio,
            Integer dailyGoalCards,
            String timezone
    ) {
        this.username = username;
        this.fullName = fullName;
        this.bio = bio;
        this.dailyGoalCards = dailyGoalCards;
        this.timezone = timezone;
    }
}
