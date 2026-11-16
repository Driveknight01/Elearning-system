package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated payload for the Explore / Landing page (featured, popular, newest sets, and root categories).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreSetsResponse {

    private List<SetSummaryResponse> featuredSets;
    private List<SetSummaryResponse> popularSets;
    private List<SetSummaryResponse> recentSets;
    private List<SetSummaryResponse.CategorySummary> categories;
}
