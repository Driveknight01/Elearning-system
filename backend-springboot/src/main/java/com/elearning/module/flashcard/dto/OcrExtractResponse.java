package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standardized OCR extraction result returned to Web and Android editors for review before saving.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrExtractResponse {

    private String rawText;
    private List<OcrExtractedCardDto> cards;
    private List<OcrTextBoxDto> boxes;
    private Double confidence;
    private String engine;
    private List<String> warnings;
}
