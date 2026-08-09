package com.notification_system.email_handler_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EmailSenderService {

    // Mock implementation — replace with real SendGrid/SES call later
    public EmailResult send(String userId, Map<String, String> payload) {
        String subject = payload.getOrDefault("subject", "(no subject)");
        String body = payload.getOrDefault("body", "");

        log.info("Sending email to user {} | subject: {} | body: {}", userId, subject, body);

        // Simulate occasional failure for testing retry logic (10% failure rate)
        if (Math.random() < 0.1) {
            return new EmailResult(false, "Simulated vendor timeout");
        }

        return new EmailResult(true, "Mock vendor accepted message");
    }

    public record EmailResult(boolean success, String vendorResponse) {}
}