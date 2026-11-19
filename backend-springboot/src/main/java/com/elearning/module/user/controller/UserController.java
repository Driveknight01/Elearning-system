package com.elearning.module.user.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.module.user.dto.AvatarUploadResponse;
import com.elearning.module.user.dto.ChangePasswordRequest;
import com.elearning.module.user.dto.MyProfileResponse;
import com.elearning.module.user.dto.PublicProfileResponse;
import com.elearning.module.user.dto.UpdatePrivacyRequest;
import com.elearning.module.user.dto.UpdateProfileRequest;
import com.elearning.module.user.dto.UserSearchItemResponse;
import com.elearning.module.user.service.UserService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for managing personal profile, avatar, password, and granular tab privacy.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users & Profile", description = "Quản lý hồ sơ cá nhân, ảnh đại diện, đổi mật khẩu và quyền riêng tư trang cá nhân")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin hồ sơ của chính người dùng đang đăng nhập")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile(currentUser.getId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        MyProfileResponse updated = userService.updateMyProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải lên ảnh đại diện mới")
    public ResponseEntity<ApiResponse<AvatarUploadResponse>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file
    ) {
        AvatarUploadResponse response = userService.uploadAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", response));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Đổi mật khẩu (hoặc đặt mật khẩu lần đầu cho tài khoản Google)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.successMessage("Đổi mật khẩu thành công"));
    }

    @PutMapping("/me/privacy")
    @Operation(summary = "Cập nhật quyền xem trang cá nhân và bật/tắt từng tab hiển thị")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updatePrivacy(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdatePrivacyRequest request
    ) {
        MyProfileResponse updated = userService.updatePrivacy(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền riêng tư thành công", updated));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm người dùng công khai theo username hoặc họ tên")
    public ResponseEntity<ApiResponse<List<UserSearchItemResponse>>> searchPublicUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.searchPublicUsers(keyword, page, size)));
    }

    @GetMapping("/profile/{username}")
    @Operation(summary = "Xem trang cá nhân theo username (tự động lọc theo quyền riêng tư)")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> getPublicProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        PublicProfileResponse profile = userService.getPublicProfile(username, currentUser);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
