package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Summary DTO for listing folders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderSummaryResponse {

    private Long id;
    private String name;
    private String description;
    private Visibility visibility;
    private String shareToken;
    private long setCount;
    private SetSummaryResponse.OwnerSummary owner;
    private Instant createdAt;
    private Instant updatedAt;
}
