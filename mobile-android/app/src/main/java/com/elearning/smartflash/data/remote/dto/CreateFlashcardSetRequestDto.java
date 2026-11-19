package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Request payload matching Spring Boot {@code CreateFlashcardSetRequest} / {@code UpdateFlashcardSetRequest}.
 */
public class CreateFlashcardSetRequestDto {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("categoryId")
    private Long categoryId;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("termLang")
    private String termLang;

    @SerializedName("defLang")
    private String defLang;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("sourceType")
    private String sourceType;

    @SerializedName("cards")
    private List<FlashcardItemRequestDto> cards;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getTermLang() {
        return termLang;
    }

    public void setTermLang(String termLang) {
        this.termLang = termLang;
    }

    public String getDefLang() {
        return defLang;
    }

    public void setDefLang(String defLang) {
        this.defLang = defLang;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public List<FlashcardItemRequestDto> getCards() {
        return cards;
    }

    public void setCards(List<FlashcardItemRequestDto> cards) {
        this.cards = cards;
    }
}
