package com.elearning.module.auth.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.module.auth.dto.AuthResponse;
import com.elearning.module.auth.dto.ForgotPasswordRequest;
import com.elearning.module.auth.dto.GoogleLoginRequest;
import com.elearning.module.auth.dto.LoginRequest;
import com.elearning.module.auth.dto.LogoutRequest;
import com.elearning.module.auth.dto.RefreshTokenRequest;
import com.elearning.module.auth.dto.RegisterRequest;
import com.elearning.module.auth.dto.ResetPasswordRequest;
import com.elearning.module.auth.service.AuthService;
import com.elearning.module.auth.service.OtpMailService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for email/password authentication, OTP password reset, refresh token, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, OTP quên mật khẩu, làm mới token và đăng xuất")
public class AuthController {

    private final AuthService authService;
    private final OtpMailService otpMailService;

    @GetMapping("/check-email")
    @Operation(summary = "Kiểm tra nhanh Email đã tồn tại hay chưa")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = authService.checkEmailExists(email);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra email thành công", exists));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới (Email/Password)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng Email hoặc Username")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập / Đăng ký bằng Google ID Token (dùng chung cho Web và Android)")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Google thành công", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Gửi mã OTP 6 số đặt lại mật khẩu qua Email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        String msg = otpMailService.isSmtpConfigured()
                ? "Mã OTP khôi phục mật khẩu đã được gửi tới email của bạn"
                : "Chế độ Local Dev (chưa bật SMTP): vui lòng lấy mã OTP 6 số trong console Backend";
        return ResponseEntity.ok(ApiResponse.successMessage(msg));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Xác nhận mã OTP và đặt lại mật khẩu mới")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.successMessage("Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới Access Token bằng Refresh Token (có xoay vòng token)")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới phiên đăng nhập thành công", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất và thu hồi Refresh Token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        authService.logout(request, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Đăng xuất thành công"));
    }
}
