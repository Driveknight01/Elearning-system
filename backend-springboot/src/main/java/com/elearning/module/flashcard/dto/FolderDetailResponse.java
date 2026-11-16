package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Detailed DTO for a folder including the flashcard sets inside it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderDetailResponse {

    private Long id;
    private String name;
    private String description;
    private Visibility visibility;
    private String shareToken;
    private long setCount;
    private boolean ownerView;
    private SetSummaryResponse.OwnerSummary owner;
    private List<SetSummaryResponse> sets;
    private Instant createdAt;
    private Instant updatedAt;
}
