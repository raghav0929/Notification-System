package com.notification_system.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_system.notification_service.dto.NotificationMessage;
import com.notification_system.notification_service.dto.NotificationRequest;
import com.notification_system.notification_service.entity.IdempotencyKey;
import com.notification_system.notification_service.entity.OutboxEvent;
import com.notification_system.notification_service.exception.DuplicateRequestException;
import com.notification_system.notification_service.repository.IdempotencyKeyRepository;
import com.notification_system.notification_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.notifications-validated}")
    private String topic;

    @Transactional
    public UUID processNotification(NotificationRequest request) {

        if (idempotencyKeyRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new DuplicateRequestException(
                    "Duplicate request for idempotencyKey: " + request.getIdempotencyKey());
        }

        UUID notificationId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        String priority = resolvePriority(request);

        // 1. Save idempotency record
        idempotencyKeyRepository.save(
                IdempotencyKey.builder()
                        .idempotencyKey(request.getIdempotencyKey())
                        .notificationId(notificationId)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 2. Build the Kafka message
        NotificationMessage message = NotificationMessage.builder()
                .notificationId(notificationId)
                .userId(request.getUserId())
                .channel(request.getChannel())
                .priority(priority)
                .type(request.getType())
                .payload(request.getPayload())
                .traceId(traceId)
                .createdAt(LocalDateTime.now())
                .build();

        // 3. Save it to the outbox table — SAME transaction as step 1
        try {
            String payloadJson = objectMapper.writeValueAsString(message);
            outboxEventRepository.save(
                    OutboxEvent.builder()
                            .id(UUID.randomUUID())
                            .notificationId(notificationId)
                            .topic(topic)
                            .kafkaKey(request.getUserId())
                            .payload(payloadJson)
                            .status("PENDING")
                            .retryCount(0)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize notification message", e);
        }

        // No direct Kafka call here anymore — the poller handles it
        return notificationId;
    }

    private String resolvePriority(NotificationRequest request) {
        if (request.getPriority() != null) return request.getPriority();
        return switch (request.getType()) {
            case "OTP" -> "HIGH";
            case "TRANSACTION_ALERT" -> "MEDIUM";
            default -> "LOW";
        };
    }
}