package com.elearning.module.group.dto;

import com.elearning.module.group.entity.StudyGroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Member item inside a study group detail response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupMemberResponse {

    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private StudyGroupRole role;
    private Instant joinedAt;
}
