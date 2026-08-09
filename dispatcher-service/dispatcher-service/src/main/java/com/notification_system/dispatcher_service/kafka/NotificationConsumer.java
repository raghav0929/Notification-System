package com.notification_system.dispatcher_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_system.dispatcher_service.dto.NotificationMessage;
import com.notification_system.dispatcher_service.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final DispatcherService dispatcherService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.notifications-validated}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String rawMessage, Acknowledgment ack) {
        try {
            NotificationMessage message = objectMapper.readValue(rawMessage, NotificationMessage.class);
            log.info("Received notification {} for user {}", message.getNotificationId(), message.getUserId());
            dispatcherService.dispatch(message, rawMessage);
            ack.acknowledge(); // only commit offset after successful processing
        } catch (Exception e) {
            log.error("Failed to process message, will retry on next poll: {}", rawMessage, e);
            // don't ack — message stays uncommitted, gets redelivered
        }
    }
}