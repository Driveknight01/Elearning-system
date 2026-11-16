package com.elearning.module.user.dto;

import com.elearning.module.user.entity.ProfilePrivacyConfig;
import com.elearning.module.user.entity.ProfileVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrivacyRequest {

    @NotNull(message = "Quyền xem trang cá nhân không được để trống")
    private ProfileVisibility profileVisibility;

    @Valid
    private ProfilePrivacyConfig profilePrivacyConfig;
}
