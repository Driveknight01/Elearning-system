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
import com.elearning.module.auth.dto.UserSummaryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Handles registration, email/username login, OTP password reset, refresh token rotation, and logout.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_EXPIRATION_MINUTES = 15;
    private static final int MAX_USERNAME_BASE_LENGTH = 40;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserStreakRepository userStreakRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final OtpMailService otpMailService;
    private final GoogleOAuth2Verifier googleOAuth2Verifier;

    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmailIgnoreCase(email.trim().toLowerCase());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role assignedRole = request.getRole() == Role.TEACHER ? Role.TEACHER : Role.USER;

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(assignedRole)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        ensureUserStreakInitialized(savedUser);
        return issueTokenPair(savedUser, request.getPlatform(), request.getDeviceInfo(), request.getFcmToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmailAndDeletedAtIsNull(identifier.toLowerCase())
                .or(() -> userRepository.findByUsernameAndDeletedAtIsNull(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        ensureAccountActive(user);
        return issueTokenPair(user, request.getPlatform(), request.getDeviceInfo(), request.getFcmToken());
    }

    /**
     * Authenticates or registers a user via Google ID Token (shared across Web React and Android Native).
     * Automatically creates {@code users}, {@code oauth_accounts}, and {@code user_streaks} on first login.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUser = googleOAuth2Verifier.verify(request.getIdToken());

        User user = oAuthAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, googleUser.subject())
                .map(oauthAccount -> handleExistingGoogleAccount(oauthAccount, googleUser))
                .orElseGet(() -> linkOrCreateGoogleUser(googleUser));

        return issueTokenPair(user, request.getPlatform(), request.getDeviceInfo(), request.getFcmToken());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String otpCode = generateSixDigitOtp();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .tokenCode(otpCode)
                .tokenType(TokenType.PASSWORD_RESET)
                .expiresAt(Instant.now().plus(OTP_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .build();

        verificationTokenRepository.save(verificationToken);
        otpMailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), otpCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        VerificationToken token = verificationTokenRepository
                .findByUserIdAndTokenCodeAndTokenTypeAndUsedAtIsNull(
                        user.getId(),
                        request.getOtpCode().trim(),
                        TokenType.PASSWORD_RESET
                )
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Mã OTP không chính xác hoặc đã được sử dụng"));

        if (token.isExpired()) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Mã OTP đã hết hạn");
        }

        token.setUsedAt(Instant.now());
        verificationTokenRepository.save(token);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active sessions after password reset for security
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken().trim());
        RefreshToken existingToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Refresh token không hợp lệ hoặc đã bị thu hồi"));

        if (existingToken.isExpired()) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new AppException(ErrorCode.INVALID_TOKEN, "Refresh token đã hết hạn");
        }

        User user = existingToken.getUser();
        if (user.isDeleted()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        ensureAccountActive(user);

        // Rotate refresh token
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        PlatformType platform = request.getPlatform() != null ? request.getPlatform() : existingToken.getPlatform();
        String deviceInfo = StringUtils.hasText(request.getDeviceInfo()) ? request.getDeviceInfo() : existingToken.getDeviceInfo();
        String fcmToken = StringUtils.hasText(request.getFcmToken()) ? request.getFcmToken() : existingToken.getFcmToken();

        return issueTokenPair(user, platform, deviceInfo, fcmToken);
    }

    @Transactional
    public void logout(LogoutRequest request, UserPrincipal currentUser) {
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            String tokenHash = hashToken(request.getRefreshToken().trim());
            refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
            return;
        }

        if (currentUser != null) {
            refreshTokenRepository.revokeAllByUserId(currentUser.getId());
        }
    }

    private User handleExistingGoogleAccount(OAuthAccount oauthAccount, GoogleUserInfo googleUser) {
        User user = oauthAccount.getUser();
        if (user.isDeleted()) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED, "Tài khoản của bạn đã bị vô hiệu hóa");
        }
        ensureAccountActive(user);

        if (!StringUtils.hasText(user.getAvatarUrl()) && StringUtils.hasText(googleUser.avatarUrl())) {
            user.setAvatarUrl(googleUser.avatarUrl());
            user = userRepository.save(user);
        }
        ensureUserStreakInitialized(user);
        return user;
    }

    private User linkOrCreateGoogleUser(GoogleUserInfo googleUser) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(googleUser.email())
                .map(existingUser -> {
                    ensureAccountActive(existingUser);
                    if (!StringUtils.hasText(existingUser.getAvatarUrl()) && StringUtils.hasText(googleUser.avatarUrl())) {
                        existingUser.setAvatarUrl(googleUser.avatarUrl());
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> createGoogleUser(googleUser));

        OAuthAccount oauthAccount = OAuthAccount.builder()
                .user(user)
                .provider(OAuthProvider.GOOGLE)
                .providerUserId(googleUser.subject())
                .build();
        oAuthAccountRepository.save(oauthAccount);

        ensureUserStreakInitialized(user);
        return user;
    }

    private User createGoogleUser(GoogleUserInfo googleUser) {
        if (userRepository.existsByEmailIgnoreCase(googleUser.email())) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED, "Tài khoản gắn với email này đã bị vô hiệu hóa");
        }

        User newUser = User.builder()
                .username(generateUniqueUsername(googleUser.email()))
                .email(googleUser.email())
                .passwordHash(null)
                .fullName(truncateFullName(googleUser.fullName()))
                .avatarUrl(googleUser.avatarUrl())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(newUser);
    }

    private void ensureUserStreakInitialized(User user) {
        if (user.getId() != null && !userStreakRepository.existsById(user.getId())) {
            UserStreak streak = UserStreak.builder()
                    .user(user)
                    .currentStreak(0)
                    .longestStreak(0)
                    .freezeCount(0)
                    .totalXp(0L)
                    .build();
            userStreakRepository.save(streak);
        }
    }

    private String generateUniqueUsername(String email) {
        String localPart = email.substring(0, email.indexOf('@'))
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("^[_.-]+|[_.-]+$", "");

        if (localPart.length() < 3) {
            localPart = "user_" + localPart;
        }
        String base = localPart.length() > MAX_USERNAME_BASE_LENGTH
                ? localPart.substring(0, MAX_USERNAME_BASE_LENGTH)
                : localPart;

        if (!userRepository.existsByUsernameIgnoreCase(base)) {
            return base;
        }

        String candidate;
        do {
            int suffix = 100_000 + SECURE_RANDOM.nextInt(900_000);
            candidate = base + "_" + suffix;
        } while (userRepository.existsByUsernameIgnoreCase(candidate));
        return candidate;
    }

    private String truncateFullName(String fullName) {
        String sanitized = StringUtils.hasText(fullName) ? fullName.trim() : "SmartFlash User";
        return sanitized.length() > MAX_FULL_NAME_LENGTH
                ? sanitized.substring(0, MAX_FULL_NAME_LENGTH)
                : sanitized;
    }

    private AuthResponse issueTokenPair(User user, PlatformType platform, String deviceInfo, String fcmToken) {
        UserPrincipal principal = UserPrincipal.from(user);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String rawRefreshToken = generateOpaqueRefreshToken();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .platform(platform != null ? platform : PlatformType.WEB)
                .deviceInfo(deviceInfo)
                .fcmToken(fcmToken)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getAccessTokenExpirationMs())
                .user(UserSummaryResponse.from(user))
                .build();
    }

    private void ensureAccountActive(User user) {
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED, "Tài khoản của bạn đã bị khóa vĩnh viễn");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            if (user.getSuspendedUntil() == null || Instant.now().isBefore(user.getSuspendedUntil())) {
                throw new AppException(ErrorCode.ACCOUNT_BANNED, "Tài khoản của bạn đang bị tạm khóa");
            }
            // Suspension period has passed; restore ACTIVE status
            user.setStatus(UserStatus.ACTIVE);
            user.setSuspendedUntil(null);
            userRepository.save(user);
        }
    }

    private String generateSixDigitOtp() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    private String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
