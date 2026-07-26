package com.notification_system.dispatcher_service.client;

import com.notification_system.dispatcher_service.dto.RateLimiterRequest;
import com.notification_system.dispatcher_service.dto.RateLimiterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class RateLimiterClient {

    private final RestClient restClient;

    public RateLimiterClient(@Value("${rate-limiter.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isAllowed(String userId, String channel) {
        try {
            RateLimiterResponse response = restClient.post()
                    .uri("/api/v1/rate-limiter/check")
                    .body(new RateLimiterRequest(userId, channel))
                    .retrieve()
                    .body(RateLimiterResponse.class);

            return response != null && response.isAllowed();
        } catch (Exception e) {
            log.error("Rate limiter call failed for user {} channel {}, failing open", userId, channel, e);
            return true; // fail-open: don't block notifications if rate limiter itself is down
        }
    }
}