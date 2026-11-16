package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO representing an individual flashcard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardResponse {

    private Long id;
    private String term;
    private String definition;
    private String hint;
    private String imageUrl;
    private String audioUrl;
    private String conceptTag;
    private Integer orderIndex;
    private Instant createdAt;
    private Instant updatedAt;
}
