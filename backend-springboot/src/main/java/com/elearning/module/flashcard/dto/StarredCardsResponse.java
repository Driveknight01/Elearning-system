package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing a user's starred card IDs and card details within a flashcard set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarredCardsResponse {

    private Long setId;
    private List<Long> starredCardIds;
    private List<FlashcardResponse> cards;
}
