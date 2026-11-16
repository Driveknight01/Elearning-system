package com.elearning.module.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to generate an AI mnemonic illustration for a flashcard term.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiImageGenerateRequest {

    @NotBlank(message = "Thuật ngữ (term) không được để trống")
    @Size(max = 200, message = "Thuật ngữ tối đa 200 ký tự")
    private String term;

    @Size(max = 500, message = "Định nghĩa tối đa 500 ký tự")
    private String definition;

    @Size(max = 50, message = "Phong cách ảnh tối đa 50 ký tự")
    private String style;

    /**
     * Optional existing flashcard ID. When provided and owned by the current user,
     * the generated image URL is persisted directly to {@code flashcards.image_url}.
     */
    private Long cardId;
}
