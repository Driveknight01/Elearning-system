package com.elearning.module.auth.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.module.auth.dto.AuthResponse;
import com.elearning.module.auth.dto.ForgotPasswordRequest;
import com.elearning.module.auth.dto.GoogleLoginRequest;
import com.elearning.module.auth.dto.LoginRequest;
import com.elearning.module.auth.dto.LogoutRequest;
import com.elearning.module.auth.dto.RefreshTokenRequest;
import com.elearning.module.auth.dto.RegisterRequest;
import com.elearning.module.auth.dto.ResetPasswordRequest;
import com.elearning.module.auth.entity.OAuthAccount;
import com.elearning.module.auth.entity.OAuthProvider;
import com.elearning.module.auth.entity.PlatformType;
import com.elearning.module.auth.entity.RefreshToken;
import com.elearning.module.auth.entity.TokenType;
import com.elearning.module.auth.entity.VerificationToken;
import com.elearning.module.auth.repository.OAuthAccountRepository;
import com.elearning.module.auth.repository.RefreshTokenRepository;
import com.elearning.module.auth.repository.VerificationTokenRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.entity.UserStreak;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.module.user.repository.UserStreakRepository;
import com.elearning.security.GoogleOAuth2Verifier;
import com.elearning.security.GoogleUserInfo;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStreakRepository userStreakRepository;
    @Mock
    private OAuthAccountRepository oAuthAccountRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private OtpMailService otpMailService;
    @Mock
    private GoogleOAuth2Verifier googleOAuth2Verifier;

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
                userStreakRepository,
                oAuthAccountRepository,
                refreshTokenRepository,
                verificationTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties,
                otpMailService,
                googleOAuth2Verifier
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

    @Test
    void loginWithGoogle_shouldCreateUserOAuthAccountAndStreakOnFirstLogin() {
        GoogleUserInfo googleUser = new GoogleUserInfo(
                "google-sub-999",
                "newbie@smartflash.vn",
                true,
                "Newbie Learner",
                "https://lh3.googleusercontent.com/pic.jpg"
        );
        when(googleOAuth2Verifier.verify("valid-google-id-token")).thenReturn(googleUser);
        when(oAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("newbie@smartflash.vn"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("newbie@smartflash.vn")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("newbie")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(77L);
            return u;
        });
        when(userStreakRepository.existsById(77L)).thenReturn(false);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("smartflash-jwt");

        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .idToken("valid-google-id-token")
                .platform(PlatformType.ANDROID)
                .deviceInfo("Pixel 8")
                .fcmToken("fcm-xyz")
                .build();

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response.getAccessToken()).isEqualTo("smartflash-jwt");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("newbie@smartflash.vn");
        assertThat(response.getUser().getUsername()).isEqualTo("newbie");
        assertThat(response.getUser().getRole()).isEqualTo(Role.USER);

        ArgumentCaptor<OAuthAccount> oauthCaptor = ArgumentCaptor.forClass(OAuthAccount.class);
        verify(oAuthAccountRepository).save(oauthCaptor.capture());
        assertThat(oauthCaptor.getValue().getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(oauthCaptor.getValue().getProviderUserId()).isEqualTo("google-sub-999");

        ArgumentCaptor<UserStreak> streakCaptor = ArgumentCaptor.forClass(UserStreak.class);
        verify(userStreakRepository).save(streakCaptor.capture());
        assertThat(streakCaptor.getValue().getCurrentStreak()).isZero();
        assertThat(streakCaptor.getValue().getLongestStreak()).isZero();
        assertThat(streakCaptor.getValue().getTotalXp()).isZero();
    }

    @Test
    void loginWithGoogle_shouldLinkExistingEmailUserWithoutCreatingDuplicateUser() {
        User existingUser = User.builder()
                .id(55L)
                .username("existing_minh")
                .email("minh@smartflash.vn")
                .passwordHash("hashed-pw")
                .fullName("Tran Minh")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build();
        GoogleUserInfo googleUser = new GoogleUserInfo(
                "google-sub-55",
                "minh@smartflash.vn",
                true,
                "Tran Minh",
                "https://lh3.googleusercontent.com/minh.jpg"
        );

        when(googleOAuth2Verifier.verify("google-token")).thenReturn(googleUser);
        when(oAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-55"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("minh@smartflash.vn"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userStreakRepository.existsById(55L)).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("linked-jwt");

        AuthResponse response = authService.loginWithGoogle(
                GoogleLoginRequest.builder().idToken("google-token").platform(PlatformType.WEB).build()
        );

        assertThat(response.getAccessToken()).isEqualTo("linked-jwt");
        assertThat(response.getUser().getId()).isEqualTo(55L);
        assertThat(response.getUser().getRole()).isEqualTo(Role.TEACHER);
        assertThat(existingUser.getAvatarUrl()).isEqualTo("https://lh3.googleusercontent.com/minh.jpg");
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
        verify(userStreakRepository, never()).save(any(UserStreak.class));
    }

    @Test
    void loginWithGoogle_shouldRejectBannedLinkedAccount() {
        User bannedUser = User.builder()
                .id(88L)
                .username("banned_google")
                .email("banned@smartflash.vn")
                .fullName("Banned")
                .role(Role.USER)
                .status(UserStatus.BANNED)
                .build();
        OAuthAccount linkedAccount = OAuthAccount.builder()
                .id(12L)
                .user(bannedUser)
                .provider(OAuthProvider.GOOGLE)
                .providerUserId("google-sub-88")
                .build();

        when(googleOAuth2Verifier.verify("banned-google-token")).thenReturn(
                new GoogleUserInfo("google-sub-88", "banned@smartflash.vn", true, "Banned", null)
        );
        when(oAuthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-88"))
                .thenReturn(Optional.of(linkedAccount));

        assertThatThrownBy(() -> authService.loginWithGoogle(
                GoogleLoginRequest.builder().idToken("banned-google-token").build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_BANNED);
    }
}
