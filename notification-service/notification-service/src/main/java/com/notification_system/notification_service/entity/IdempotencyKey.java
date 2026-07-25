package com.notification_system.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}