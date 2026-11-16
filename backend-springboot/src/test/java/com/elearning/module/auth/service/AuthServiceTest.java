package com.elearning.module.auth.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.module.auth.dto.AuthResponse;
import com.elearning.module.auth.dto.ForgotPasswordRequest;
import com.elearning.module.auth.dto.LoginRequest;
import com.elearning.module.auth.dto.LogoutRequest;
import com.elearning.module.auth.dto.RefreshTokenRequest;
import com.elearning.module.auth.dto.RegisterRequest;
import com.elearning.module.auth.dto.ResetPasswordRequest;
import com.elearning.module.auth.entity.PlatformType;
import com.elearning.module.auth.entity.RefreshToken;
import com.elearning.module.auth.entity.TokenType;
import com.elearning.module.auth.entity.VerificationToken;
import com.elearning.module.auth.repository.RefreshTokenRepository;
import com.elearning.module.auth.repository.VerificationTokenRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.JwtProperties;
import com.elearning.security.JwtTokenProvider;
import com.elearning.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private OtpMailService otpMailService;

    private PasswordEncoder passwordEncoder;
    private JwtProperties jwtProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenExpirationMs(86_400_000L);
        jwtProperties.setRefreshTokenExpirationMs(604_800_000L);

        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                verificationTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties,
                otpMailService
        );
    }

    @Test
    void register_shouldForceUserRoleWhenAdminRequestedAndIssueTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .username("minh_student")
                .email("Minh@SmartFlash.vn")
                .password("Secret123")
                .fullName("Tran Minh")
                .role(Role.ADMIN)
                .platform(PlatformType.WEB)
                .build();

        when(userRepository.existsByEmailIgnoreCase("minh@smartflash.vn")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("minh_student")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("jwt-access-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getRole()).isEqualTo(Role.USER);
        assertThat(response.getUser().getEmail()).isEqualTo("minh@smartflash.vn");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("new_user")
                .email("dup@smartflash.vn")
                .password("Secret123")
                .fullName("Dup")
                .build();

        when(userRepository.existsByEmailIgnoreCase("dup@smartflash.vn")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void login_shouldThrowWhenAccountIsBanned() {
        User banned = User.builder()
                .id(5L)
                .username("banned_user")
                .email("banned@smartflash.vn")
                .passwordHash(passwordEncoder.encode("Secret123"))
                .fullName("Banned")
                .role(Role.USER)
                .status(UserStatus.BANNED)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("banned@smartflash.vn"))
                .thenReturn(Optional.of(banned));

        LoginRequest request = LoginRequest.builder()
                .identifier("banned@smartflash.vn")
                .password("Secret123")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_BANNED);
    }

    @Test
    void forgotAndResetPassword_shouldUpdatePasswordAndRevokeAllTokens() {
        User user = User.builder()
                .id(11L)
                .username("hoa")
                .email("hoa@smartflash.vn")
                .passwordHash(passwordEncoder.encode("OldPass123"))
                .fullName("Le Hoa")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("hoa@smartflash.vn")).thenReturn(Optional.of(user));

        authService.forgotPassword(new ForgotPasswordRequest("hoa@smartflash.vn"));

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(tokenCaptor.capture());
        VerificationToken savedOtp = tokenCaptor.getValue();
        assertThat(savedOtp.getTokenCode()).hasSize(6);
        verify(otpMailService).sendPasswordResetOtp(eq("hoa@smartflash.vn"), eq("Le Hoa"), eq(savedOtp.getTokenCode()));

        when(verificationTokenRepository.findByUserIdAndTokenCodeAndTokenTypeAndUsedAtIsNull(
                11L, savedOtp.getTokenCode(), TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(savedOtp));

        authService.resetPassword(new ResetPasswordRequest("hoa@smartflash.vn", savedOtp.getTokenCode(), "NewPass456"));

        assertThat(passwordEncoder.matches("NewPass456", user.getPasswordHash())).isTrue();
        assertThat(savedOtp.isUsed()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(11L);
    }

    @Test
    void refreshToken_shouldRotateTokenAndIssueNewAccessToken() {
        User user = User.builder()
                .id(20L)
                .username("lan")
                .email("lan@smartflash.vn")
                .fullName("Nguyen Lan")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build();

        String rawToken = "sample-raw-refresh-token";
        RefreshToken stored = RefreshToken.builder()
                .id(100L)
                .user(user)
                .tokenHash(AuthService.hashToken(rawToken))
                .platform(PlatformType.ANDROID)
                .expiresAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(AuthService.hashToken(rawToken)))
                .thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new-jwt-access-token");

        AuthResponse response = authService.refreshToken(
                new RefreshTokenRequest(rawToken, null, null, null));

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("new-jwt-access-token");
        assertThat(response.getRefreshToken()).isNotEqualTo(rawToken);
    }

    @Test
    void logout_shouldRevokeProvidedRefreshToken() {
        String rawToken = "logout-token";
        RefreshToken stored = RefreshToken.builder()
                .id(101L)
                .tokenHash(AuthService.hashToken(rawToken))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(AuthService.hashToken(rawToken)))
                .thenReturn(Optional.of(stored));

        authService.logout(new LogoutRequest(rawToken), null);

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }
}
