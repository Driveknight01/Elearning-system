package com.elearning.module.group.dto;

import com.elearning.module.flashcard.dto.SetSummaryResponse;
import com.elearning.module.group.entity.StudyGroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Detailed DTO for a study group including its members and shared flashcard sets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private StudyGroupRole myRole;
    private SetSummaryResponse.OwnerSummary creator;
    private List<StudyGroupMemberResponse> members;
    private List<StudyGroupSharedSetResponse> sharedSets;
    private Instant createdAt;
}
