package com.elearning.module.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating an individual flashcard inside a set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardItemRequest {

    /** Existing card ID when updating a set; null for newly added cards. */
    private Long id;

    @NotBlank(message = "Thuật ngữ (term) không được để trống")
    private String term;

    @NotBlank(message = "Định nghĩa (definition) không được để trống")
    private String definition;

    @Size(max = 500, message = "Gợi ý tối đa 500 ký tự")
    private String hint;

    private String imageUrl;

    private String audioUrl;

    @Size(max = 100, message = "Nhãn khái niệm tối đa 100 ký tự")
    private String conceptTag;

    private Integer orderIndex;
}
