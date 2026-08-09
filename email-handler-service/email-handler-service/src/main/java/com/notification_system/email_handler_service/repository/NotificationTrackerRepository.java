package com.notification_system.email_handler_service.repository;

import com.notification_system.email_handler_service.entity.NotificationTracker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationTrackerRepository extends JpaRepository<NotificationTracker, UUID> {
}