package com.elearning.module.auth.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Dispatches OTP verification emails via SMTP with a local-dev console log fallback
 * when SMTP credentials are not configured yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@smartflash.local}")
    private String mailFrom;

    @Value("${spring.mail.password:changeme}")
    private String mailPassword;

    public boolean isSmtpConfigured() {
        return StringUtils.hasText(mailFrom)
                && !mailFrom.endsWith(".local")
                && !"your-email@gmail.com".equalsIgnoreCase(mailFrom.trim())
                && StringUtils.hasText(mailPassword)
                && !"changeme".equals(mailPassword)
                && !"your-16-char-app-password".equals(mailPassword);
    }

    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        if (!isSmtpConfigured()) {
            log.warn("==========================================================");
            log.warn("[LOCAL DEV OTP] Chưa cấu hình SMTP Gmail.");
            log.warn("[LOCAL DEV OTP] Mã OTP đặt lại mật khẩu cho {} ({}): {}", fullName, toEmail, otpCode);
            log.warn("==========================================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("[SmartFlash] Mã OTP khôi phục mật khẩu");
            message.setText(String.format(
                    "Xin chào %s,\n\nMã OTP đặt lại mật khẩu của bạn là: %s\nMã có hiệu lực trong 15 phút.\n\nSmartFlash Team",
                    fullName, otpCode
            ));
            mailSender.send(message);
            log.info("Sent password reset OTP email to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send OTP email to {} via SMTP: {}", toEmail, ex.getMessage(), ex);
            throw new AppException(
                    ErrorCode.UNCATEGORIZED_ERROR,
                    "Không thể gửi email OTP qua SMTP. Vui lòng kiểm tra MAIL_USERNAME và Gmail App Password."
            );
        }
    }
}
