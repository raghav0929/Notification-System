package com.notification_system.notification_service.kafka;

import com.notification_system.notification_service.entity.OutboxEvent;
import com.notification_system.notification_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate; // raw String template, see config note below

    @Scheduled(fixedDelay = 10000) // every 3 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getKafkaKey(), event.getPayload()).get();
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                log.info("Published outbox event {} for notification {}", event.getId(), event.getNotificationId());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                log.error("Failed to publish outbox event {} (attempt {})", event.getId(), event.getRetryCount(), e);
                // leave status as PENDING — will retry next poll
            }
            outboxEventRepository.save(event);
        }
    }
}