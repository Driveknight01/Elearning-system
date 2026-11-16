package com.elearning.module.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for joining a study group via invite code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinStudyGroupRequest {

    @NotBlank(message = "Mã mời nhóm học không được để trống")
    @Size(max = 15, message = "Mã mời tối đa 15 ký tự")
    private String inviteCode;
}
