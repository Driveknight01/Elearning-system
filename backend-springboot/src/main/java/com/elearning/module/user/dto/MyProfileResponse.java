package com.elearning.module.user.dto;

import com.elearning.module.user.entity.ProfilePrivacyConfig;
import com.elearning.module.user.entity.ProfileVisibility;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.entity.UserStreak;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Full profile details returned to the authenticated owner ({@code GET /api/v1/users/me}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private Role role;
    private boolean verifiedTeacher;
    private UserStatus status;
    private Integer dailyGoalCards;
    private String timezone;
    private ProfileVisibility profileVisibility;
    private ProfilePrivacyConfig profilePrivacyConfig;
    private boolean hasPassword;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long totalXp;
    private Instant createdAt;

    public static MyProfileResponse from(User user, UserStreak streak) {
        return MyProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .verifiedTeacher(user.isVerifiedTeacher())
                .status(user.getStatus())
                .dailyGoalCards(user.getDailyGoalCards())
                .timezone(user.getTimezone())
                .profileVisibility(user.getProfileVisibility())
                .profilePrivacyConfig(user.getProfilePrivacyConfig())
                .hasPassword(StringUtils.hasText(user.getPasswordHash()))
                .currentStreak(streak != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak != null ? streak.getLongestStreak() : 0)
                .totalXp(streak != null ? streak.getTotalXp() : 0L)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
