package com.elearning.module.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload containing the AI-generated mnemonic image URL and optional persisted card ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiImageGenerateResponse {

    private String imageUrl;
    private String prompt;
    private String provider;
    private Long savedToCardId;
}
