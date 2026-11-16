package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.ModerationStatus;
import com.elearning.module.flashcard.entity.SourceType;
import com.elearning.module.flashcard.entity.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detailed DTO for a flashcard set including its ordered child flashcards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Visibility visibility;
    private ModerationStatus moderationStatus;
    private boolean featured;
    private String shareToken;
    private String termLang;
    private String defLang;
    private List<String> tags;
    private SourceType sourceType;
    private Long clonedFromSetId;
    private Integer cardCount;
    private Integer viewCount;
    private Integer cloneCount;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private boolean ownerView;
    private SetSummaryResponse.OwnerSummary owner;
    private SetSummaryResponse.CategorySummary category;
    private List<FlashcardResponse> cards;
    private Instant createdAt;
    private Instant updatedAt;
}
