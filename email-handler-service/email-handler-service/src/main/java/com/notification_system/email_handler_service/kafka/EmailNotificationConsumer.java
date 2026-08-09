package com.notification_system.email_handler_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_system.email_handler_service.dto.NotificationMessage;
import com.notification_system.email_handler_service.service.EmailHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final EmailHandlerService emailHandlerService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.notifications-email}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String rawMessage, Acknowledgment ack) {
        try {
            NotificationMessage message = objectMapper.readValue(rawMessage, NotificationMessage.class);
            log.info("Received email notification {} for user {}", message.getNotificationId(), message.getUserId());
            emailHandlerService.handle(message);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process email notification, will retry on next poll: {}", rawMessage, e);
            // don't ack — Kafka redelivers
        }
    }
}