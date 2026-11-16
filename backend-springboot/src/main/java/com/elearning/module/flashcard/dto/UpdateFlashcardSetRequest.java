package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for updating a flashcard set and synchronizing its child cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlashcardSetRequest {

    @NotBlank(message = "Tiêu đề bộ thẻ không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    private String description;

    private Long categoryId;

    private Visibility visibility;

    @Size(max = 10, message = "Mã ngôn ngữ thuật ngữ tối đa 10 ký tự")
    private String termLang;

    @Size(max = 10, message = "Mã ngôn ngữ định nghĩa tối đa 10 ký tự")
    private String defLang;

    private List<String> tags;

    /** When true, rotates {@code share_token} to invalidate previously shared links. */
    private boolean regenerateShareToken;

    @NotEmpty(message = "Bộ thẻ phải có ít nhất 1 thẻ")
    @Valid
    private List<FlashcardItemRequest> cards;
}
