package com.notification_system.notification_service.dto;


import lombok.*;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "channel is required")
    private String channel;

    private String priority;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    private Map<String, String> payload;
}