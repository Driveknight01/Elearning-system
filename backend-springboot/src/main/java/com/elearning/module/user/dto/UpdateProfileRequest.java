package com.elearning.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Tên đăng nhập chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
    private String username;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 500, message = "Giới thiệu bản thân tối đa 500 ký tự")
    private String bio;

    @Min(value = 1, message = "Mục tiêu mỗi ngày tối thiểu 1 thẻ")
    @Max(value = 500, message = "Mục tiêu mỗi ngày tối đa 500 thẻ")
    private Integer dailyGoalCards;

    @Size(max = 50, message = "Múi giờ tối đa 50 ký tự")
    private String timezone;
}
