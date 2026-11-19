package com.elearning.module.user.dto;

import com.elearning.module.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO returned when searching public user profiles by keyword.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchItemResponse {

    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private boolean verifiedTeacher;
    private long setCount;
    private long classCount;
}
