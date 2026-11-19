package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response item matching Spring Boot {@code FlashcardResponse}.
 */
public class FlashcardItemDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("term")
    private String term;

    @SerializedName("definition")
    private String definition;

    @SerializedName("hint")
    private String hint;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("audioUrl")
    private String audioUrl;

    @SerializedName("conceptTag")
    private String conceptTag;

    @SerializedName("orderIndex")
    private Integer orderIndex;

    public Long getId() {
        return id;
    }

    public String getTerm() {
        return term;
    }

    public String getDefinition() {
        return definition;
    }

    public String getHint() {
        return hint;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getConceptTag() {
        return conceptTag;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }
}
