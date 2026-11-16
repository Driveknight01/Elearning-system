package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Public rating & review item for a flashcard set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetRatingItemResponse {

    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Short rating;
    private String reviewComment;
    private Instant ratedAt;
}
