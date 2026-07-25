package com.notification_system.notification_service.repository;

import com.notification_system.notification_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, java.util.UUID> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}