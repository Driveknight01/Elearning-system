package com.elearning.module.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized (0.0 - 1.0) bounding box for an OCR-detected text region on the uploaded image.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrTextBoxDto {

    private String id;
    private String text;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Double angle;
    private Double confidence;
}
