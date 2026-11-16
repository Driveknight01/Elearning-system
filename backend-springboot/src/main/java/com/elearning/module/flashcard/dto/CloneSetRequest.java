package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.Visibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional request body when cloning (forking) an existing flashcard set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneSetRequest {

    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    private Visibility visibility;
}
