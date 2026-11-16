package com.elearning.module.group.dto;

import com.elearning.module.flashcard.dto.SetSummaryResponse;
import com.elearning.module.group.entity.StudyGroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Summary DTO for listing a user's study groups.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupSummaryResponse {

    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private long memberCount;
    private int setCount;
    private StudyGroupRole myRole;
    private SetSummaryResponse.OwnerSummary creator;
    private Instant createdAt;
}
