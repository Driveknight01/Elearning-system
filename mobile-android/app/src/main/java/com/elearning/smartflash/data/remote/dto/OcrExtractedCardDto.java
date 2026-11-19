package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Single term-definition pair extracted from an uploaded document image via OCR.
 */
public class OcrExtractedCardDto {

    @SerializedName("term")
    private String term;

    @SerializedName("definition")
    private String definition;

    public OcrExtractedCardDto() {
    }

    public OcrExtractedCardDto(String term, String definition) {
        this.term = term;
        this.definition = definition;
    }

    public String getTerm() {
        return term;
    }

    public String getDefinition() {
        return definition;
    }
}
