package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Standardized OCR extraction response matching Spring Boot {@code OcrExtractResponse}.
 */
public class OcrExtractResponseDto {

    @SerializedName("rawText")
    private String rawText;

    @SerializedName("cards")
    private List<OcrExtractedCardDto> cards;

    @SerializedName("boxes")
    private List<OcrTextBoxDto> boxes;

    @SerializedName("confidence")
    private Double confidence;

    @SerializedName("engine")
    private String engine;

    @SerializedName("warnings")
    private List<String> warnings;

    public String getRawText() {
        return rawText;
    }

    public List<OcrExtractedCardDto> getCards() {
        return cards;
    }

    public List<OcrTextBoxDto> getBoxes() {
        return boxes;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getEngine() {
        return engine;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
