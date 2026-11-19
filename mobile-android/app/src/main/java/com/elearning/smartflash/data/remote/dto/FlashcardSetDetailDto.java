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

    @SerializedName("viewCount")
    private Integer viewCount;

    @SerializedName("cloneCount")
    private Integer cloneCount;

    @SerializedName("avgRating")
    private Double avgRating;

    @SerializedName("ratingCount")
    private Integer ratingCount;

    @SerializedName("ownerView")
    private boolean ownerView;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("owner")
    private SetSummaryDto.OwnerSummaryDto owner;

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

    public Integer getViewCount() {
        return viewCount;
    }

    public Integer getCloneCount() {
        return cloneCount;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public boolean isOwnerView() {
        return ownerView;
    }

    public List<String> getTags() {
        return tags;
    }

    public SetSummaryDto.OwnerSummaryDto getOwner() {
        return owner;
    }

    public List<FlashcardItemDto> getCards() {
        return cards;
    }
}
