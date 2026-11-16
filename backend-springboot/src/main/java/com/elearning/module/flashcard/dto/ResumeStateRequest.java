package com.elearning.module.flashcard.dto;

import com.elearning.module.flashcard.entity.StudyMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request payload for saving cross-platform study resume state ({@code resume_state_json}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeStateRequest {

    @NotNull(message = "Chế độ học không được để trống")
    private StudyMode studyMode;

    @NotNull(message = "Trạng thái phiên học không được để trống")
    private Map<String, Object> resumeStateJson;
}
