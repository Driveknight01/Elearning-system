package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.StudyMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO representing a user's interaction with a flashcard set (Saved, Recents, Rating, Resume State).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetInteractionResponse {

    private Long setId;
    private boolean saved;
    private StudyMode lastStudyMode;
    private Instant lastAccessedAt;
    private Map<String, Object> resumeStateJson;
    private Short rating;
    private String reviewComment;
    private Instant ratedAt;
    private SetSummaryResponse set;
}
