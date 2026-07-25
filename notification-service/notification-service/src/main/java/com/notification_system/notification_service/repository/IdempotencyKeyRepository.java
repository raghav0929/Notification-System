package com.notification_system.notification_service.repository;

import com.notification_system.notification_service.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}