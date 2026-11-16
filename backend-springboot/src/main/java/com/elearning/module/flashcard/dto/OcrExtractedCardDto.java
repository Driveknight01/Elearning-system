package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single term-definition pair extracted from an uploaded document image via OCR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrExtractedCardDto {

    private String term;
    private String definition;
}
