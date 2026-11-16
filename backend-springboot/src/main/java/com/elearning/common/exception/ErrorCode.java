package com.elearning.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized error codes, HTTP statuses, and default messages for SmartFlash APIs.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // System & Request Errors
    UNCATEGORIZED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "Lỗi hệ thống không xác định"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "REQ_400", "Yêu cầu không hợp lệ"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "REQ_400_VALIDATION", "Dữ liệu đầu vào không hợp lệ"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "REQ_400_MISSING_PARAM", "Thiếu tham số bắt buộc"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "REQ_429", "Bạn đã gửi quá nhiều yêu cầu, vui lòng thử lại sau"),

    // Auth & Security Errors
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "Chưa xác thực hoặc phiên đăng nhập đã hết hạn"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không chính xác"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_TOKEN", "Token không hợp lệ hoặc đã hết hạn"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "Bạn không có quyền thực hiện thao tác này"),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "AUTH_403_BANNED", "Tài khoản của bạn đã bị tạm khóa hoặc vô hiệu hóa"),

    // Resource & Domain Errors
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES_404", "Không tìm thấy tài nguyên yêu cầu"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "Không tìm thấy người dùng"),
    SET_NOT_FOUND(HttpStatus.NOT_FOUND, "SET_404", "Không tìm thấy bộ thẻ"),
    CONFLICT(HttpStatus.CONFLICT, "RES_409", "Dữ liệu đã tồn tại"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_EMAIL", "Email đã được sử dụng"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_USERNAME", "Tên đăng nhập đã được sử dụng"),

    // Storage & External AI Service Errors
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_413", "Dung lượng tệp vượt quá giới hạn cho phép"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_500", "Không thể lưu trữ tệp tải lên"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_503", "Dịch vụ AI/ML tạm thời không khả dụng");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
