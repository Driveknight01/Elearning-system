package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a folder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderRequest {

    @NotBlank(message = "Tên thư mục không được để trống")
    @Size(max = 150, message = "Tên thư mục tối đa 150 ký tự")
    private String name;

    private String description;

    private Visibility visibility;

    /** Optional flag used on update to rotate {@code share_token}. */
    private boolean regenerateShareToken;
}
