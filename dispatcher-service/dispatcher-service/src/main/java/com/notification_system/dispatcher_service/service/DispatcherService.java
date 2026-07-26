package com.notification_system.dispatcher_service.service;

import com.notification_system.dispatcher_service.client.RateLimiterClient;
import com.notification_system.dispatcher_service.dto.NotificationMessage;
import com.notification_system.dispatcher_service.entity.UserPreference;
import com.notification_system.dispatcher_service.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatcherService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final RateLimiterClient rateLimiterClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.notifications-email}")
    private String emailTopic;

    public void dispatch(NotificationMessage message, String rawPayload) {

        String channel = message.getChannel();
        String userId = message.getUserId();

        // 1. Check preferences
        Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserIdAndChannel(userId, channel);

        if (prefOpt.isPresent()) {
            UserPreference pref = prefOpt.get();

            if (Boolean.FALSE.equals(pref.getOptedIn())) {
                log.info("User {} opted out of channel {}, skipping notification {}",
                        userId, channel, message.getNotificationId());
                return;
            }

            if (isInQuietHours(pref)) {
                log.info("User {} is in quiet hours for channel {}, skipping notification {}",
                        userId, channel, message.getNotificationId());
                return;
            }
        }
        // If no preference row exists, default to allowed (opt-out model)

        // 2. Rate limiter check
        if (!rateLimiterClient.isAllowed(userId, channel)) {
            log.warn("Rate limit exceeded for user {} channel {}, dropping notification {}",
                    userId, channel, message.getNotificationId());
            return;
        }

        // 3. Route to channel-specific topic
        String targetTopic = resolveTopic(channel);
        kafkaTemplate.send(targetTopic, userId, rawPayload);
        log.info("Dispatched notification {} to topic {}", message.getNotificationId(), targetTopic);
    }

    private boolean isInQuietHours(UserPreference pref) {
        if (pref.getQuietHoursStart() == null || pref.getQuietHoursEnd() == null) return false;
        LocalTime now = LocalTime.now();
        LocalTime start = pref.getQuietHoursStart();
        LocalTime end = pref.getQuietHoursEnd();

        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        } else {
            // quiet hours span midnight, e.g. 22:00 - 07:00
            return !now.isBefore(start) || !now.isAfter(end);
        }
    }

    private String resolveTopic(String channel) {
        return switch (channel) {
            case "EMAIL" -> emailTopic;
            default -> throw new IllegalArgumentException("Unsupported channel: " + channel);
        };
    }
}