package com.notification_system.dispatcher_service.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RateLimiterResponse {
    private boolean allowed;
    private int retryAfterSeconds;
}