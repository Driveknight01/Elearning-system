package com.elearning.module.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a peer study group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudyGroupRequest {

    @NotBlank(message = "Tên nhóm học không được để trống")
    @Size(max = 150, message = "Tên nhóm học tối đa 150 ký tự")
    private String name;

    private String description;
}
