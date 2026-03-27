package com.example.restaurantpro.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;

    @Value("${app.mail.from-name:Restaurant Pro}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public void sendOtpEmail(String toEmail, String otpCode, long expiryMinutes) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đăng nhập nhà hàng Rivière");
            helper.setText("Mã OTP của bạn là: " + otpCode + "\n\nMã có hiệu lực trong " + expiryMinutes + " phút.", false);

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            }

            mailSender.send(message);
        } catch (MailException ex) {
            throw ex;
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new MailSendException("Không thể gửi email OTP.", ex);
        }
    }
}