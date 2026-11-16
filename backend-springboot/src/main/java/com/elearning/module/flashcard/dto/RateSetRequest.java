package com.elearning.module.flashcard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for rating (1-5 stars) and reviewing a flashcard set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateSetRequest {

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    private Short rating;

    @Size(max = 500, message = "Nhận xét tối đa 500 ký tự")
    private String reviewComment;
}
