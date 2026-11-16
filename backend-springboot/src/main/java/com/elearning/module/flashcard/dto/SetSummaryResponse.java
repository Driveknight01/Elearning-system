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
 * Summary DTO for listing flashcard sets in personal library or search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetSummaryResponse {

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
    private Integer cardCount;
    private Integer viewCount;
    private Integer cloneCount;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private OwnerSummary owner;
    private CategorySummary category;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerSummary {
        private Long id;
        private String username;
        private String fullName;
        private String avatarUrl;
        private boolean verifiedTeacher;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
        private String slug;
        private String colorHex;
    }
}
