package com.elearning.module.group.dto;

import com.elearning.module.flashcard.dto.SetSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Shared flashcard set item inside a study group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupSharedSetResponse {

    private SetSummaryResponse set;
    private Long sharedByUserId;
    private String sharedByUsername;
    private String sharedByFullName;
    private Instant sharedAt;
}
