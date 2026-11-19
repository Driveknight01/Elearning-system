package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request item matching Spring Boot {@code FlashcardItemRequest}.
 */
public class FlashcardItemRequestDto {

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

    public FlashcardItemRequestDto() {
    }

    public FlashcardItemRequestDto(Long id, String term, String definition, String imageUrl, Integer orderIndex) {
        this.id = id;
        this.term = term;
        this.definition = definition;
        this.imageUrl = imageUrl;
        this.orderIndex = orderIndex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getConceptTag() {
        return conceptTag;
    }

    public void setConceptTag(String conceptTag) {
        this.conceptTag = conceptTag;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
