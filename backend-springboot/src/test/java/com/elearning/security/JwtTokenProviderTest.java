package com.elearning.security;

import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("SmartFlashSuperSecretKeyForJwtSigning2026MustBeAtLeast256BitsLong!");
        jwtProperties.setAccessTokenExpirationMs(60_000L);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.initKey();
    }

    @Test
    void generateAndValidateAccessToken_shouldExtractUserIdAndRole() {
        User user = User.builder()
                .id(42L)
                .username("teacher_an")
                .email("an@smartflash.vn")
                .passwordHash("encoded")
                .fullName("Nguyen Van An")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build();

        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("TEACHER");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedOrExpiredToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();

        jwtProperties.setAccessTokenExpirationMs(-1_000L);
        User user = User.builder()
                .id(7L)
                .username("expired_user")
                .email("expired@smartflash.vn")
                .fullName("Expired")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        String expiredToken = jwtTokenProvider.generateAccessToken(UserPrincipal.from(user));
        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    void userPrincipal_shouldReflectBannedAndSuspendedAccountStatus() {
        User bannedUser = User.builder()
                .id(1L)
                .username("banned")
                .email("banned@smartflash.vn")
                .fullName("Banned")
                .role(Role.USER)
                .status(UserStatus.BANNED)
                .build();
        assertThat(UserPrincipal.from(bannedUser).isAccountNonLocked()).isFalse();

        User suspendedUser = User.builder()
                .id(2L)
                .username("suspended")
                .email("suspended@smartflash.vn")
                .fullName("Suspended")
                .role(Role.USER)
                .status(UserStatus.SUSPENDED)
                .suspendedUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        assertThat(UserPrincipal.from(suspendedUser).isAccountNonLocked()).isFalse();
    }
}
