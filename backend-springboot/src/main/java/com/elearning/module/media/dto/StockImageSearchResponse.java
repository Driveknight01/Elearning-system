package com.elearning.module.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload for stock illustration search by flashcard term.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockImageSearchResponse {

    private String query;
    private String provider;
    private List<StockImageItemDto> items;
}
