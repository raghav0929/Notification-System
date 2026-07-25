package com.notification_system.notification_service.controller;

import com.notification_system.notification_service.dto.NotificationRequest;
import com.notification_system.notification_service.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(
            @Valid @RequestBody NotificationRequest request) {

        UUID notificationId = notificationService.processNotification(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "notificationId", notificationId,
                        "status", "ACCEPTED"
                ));
    }
}