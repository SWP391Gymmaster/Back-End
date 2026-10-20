package com.swp391_be.service;

import com.swp391_be.dto.VerificationEmailStatus;
import com.swp391_be.service.EmailVerificationService.VerificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationEmailService {
    private static final Logger log = LoggerFactory.getLogger(VerificationEmailService.class);
    private final JavaMailSender sender;
    private final EmailVerificationService verification;
    private final String baseUrl;
    private final String from;

    public VerificationEmailService(JavaMailSender sender, EmailVerificationService verification,
            @Value("${app.frontend.base-url:http://localhost:5173}") String baseUrl,
            @Value("${app.mail.from:no-reply@example.com}") String from) {
        this.sender = sender;
        this.verification = verification;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.from = from;
    }

    public VerificationEmailStatus send(VerificationDelivery delivery) {
        if (!verification.isCurrent(delivery)) return VerificationEmailStatus.FAILED;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(delivery.email());
            message.setSubject("Verify your email address");
            message.setText("Verify your email within 24 hours: " + baseUrl
                    + "/verify-email?token=" + delivery.rawToken()
                    + "\nIf you did not request this account, you can ignore this message.");
            sender.send(message);
            return VerificationEmailStatus.SENT;
        } catch (RuntimeException error) {
            log.warn("Verification email submission failed for user {}", delivery.userId());
            return VerificationEmailStatus.FAILED;
        }
    }
}
