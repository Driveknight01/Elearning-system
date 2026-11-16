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
import com.elearning.module.auth.dto.UserSummaryResponse;
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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final OtpMailService otpMailService;

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
