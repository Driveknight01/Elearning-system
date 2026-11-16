package com.elearning.module.user.entity;

import com.elearning.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * Core user account entity mapped to {@code users}.
 * Supports email/password and Google OAuth2 accounts, RBAC roles, and granular profile privacy.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Builder.Default
    @Column(name = "is_verified_teacher", nullable = false)
    private boolean verifiedTeacher = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Builder.Default
    @Column(name = "daily_goal_cards", nullable = false)
    private Integer dailyGoalCards = 20;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 20)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_privacy_config", nullable = false, columnDefinition = "jsonb")
    private ProfilePrivacyConfig profilePrivacyConfig = new ProfilePrivacyConfig();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fsrs_weights", columnDefinition = "jsonb")
    private List<Double> fsrsWeights;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
