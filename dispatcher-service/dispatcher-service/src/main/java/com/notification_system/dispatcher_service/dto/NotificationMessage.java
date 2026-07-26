package com.notification_system.dispatcher_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationMessage {

    private UUID notificationId;
    private String userId;
    private String channel;
    private String priority;
    private String type;
    private Map<String, String> payload;
    private UUID traceId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}