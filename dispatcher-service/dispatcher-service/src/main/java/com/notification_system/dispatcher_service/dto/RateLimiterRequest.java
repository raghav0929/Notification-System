package com.notification_system.dispatcher_service.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RateLimiterRequest {
    private String userId;
    private String channel;
}