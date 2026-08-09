package com.notification_system.email_handler_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_tracker")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationTracker {

    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    private String channel;

    private String status; // SENT, FAILED, RETRYING

    private Integer attempts;

    @Column(name = "vendor_response", columnDefinition = "TEXT")
    private String vendorResponse;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}