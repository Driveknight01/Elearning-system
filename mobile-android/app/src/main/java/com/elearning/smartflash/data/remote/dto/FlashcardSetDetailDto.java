package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Detailed flashcard set response matching Spring Boot {@code SetDetailResponse}.
 */
public class FlashcardSetDetailDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("shareToken")
    private String shareToken;

    @SerializedName("termLang")
    private String termLang;

    @SerializedName("defLang")
    private String defLang;

    @SerializedName("cardCount")
    private Integer cardCount;

    @SerializedName("cards")
    private List<FlashcardItemDto> cards;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
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

    public String getTermLang() {
        return termLang;
    }

    public String getDefLang() {
        return defLang;
    }

    public Integer getCardCount() {
        return cardCount;
    }

    public List<FlashcardItemDto> getCards() {
        return cards;
    }
}
