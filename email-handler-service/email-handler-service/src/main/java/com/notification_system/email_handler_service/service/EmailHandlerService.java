package com.notification_system.email_handler_service.service;

import com.notification_system.email_handler_service.dto.NotificationMessage;
import com.notification_system.email_handler_service.entity.NotificationTracker;
import com.notification_system.email_handler_service.repository.NotificationTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailHandlerService {

    private final NotificationTrackerRepository trackerRepository;
    private final EmailSenderService emailSenderService;

    private static final int MAX_ATTEMPTS = 3;

    public void handle(NotificationMessage message) {

        NotificationTracker tracker = trackerRepository.findById(message.getNotificationId())
                .orElse(NotificationTracker.builder()
                        .notificationId(message.getNotificationId())
                        .channel(message.getChannel())
                        .status("PROCESSING")
                        .attempts(0)
                        .build());

        int currentAttempt = tracker.getAttempts() + 1;
        tracker.setAttempts(currentAttempt);

        EmailSenderService.EmailResult result = emailSenderService.send(message.getUserId(), message.getPayload());

        if (result.success()) {
            tracker.setStatus("SENT");
            tracker.setVendorResponse(result.vendorResponse());
            log.info("Email sent successfully for notification {}", message.getNotificationId());
        } else {
            if (currentAttempt >= MAX_ATTEMPTS) {
                tracker.setStatus("FAILED");
                log.error("Notification {} failed after {} attempts, moving to DLQ",
                        message.getNotificationId(), currentAttempt);
                // In a fuller implementation, publish to notifications.dlq here
            } else {
                tracker.setStatus("RETRYING");
                log.warn("Notification {} failed attempt {}, will retry",
                        message.getNotificationId(), currentAttempt);
            }
            tracker.setVendorResponse(result.vendorResponse());
        }

        tracker.setUpdatedAt(LocalDateTime.now());
        trackerRepository.save(tracker);

        // Throw if we should trigger a Kafka redelivery (i.e., still retrying)
        if ("RETRYING".equals(tracker.getStatus())) {
            throw new RuntimeException("Email send failed, will retry via Kafka redelivery: " + result.vendorResponse());
        }
    }
}