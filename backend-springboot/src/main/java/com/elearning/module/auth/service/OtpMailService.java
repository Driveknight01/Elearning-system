package com.elearning.module.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Dispatches OTP verification emails with a local-dev log fallback when SMTP is not configured.
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

    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode) {
        if ("changeme".equals(mailPassword) || mailFrom.endsWith(".local")) {
            log.info("[LOCAL DEV OTP] Password reset OTP for {} ({}): {}", fullName, toEmail, otpCode);
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
        } catch (MailException ex) {
            log.warn("Failed to send OTP email to {} ({}). Falling back to server log. OTP={}",
                    toEmail, ex.getMessage(), otpCode);
        }
    }
}
