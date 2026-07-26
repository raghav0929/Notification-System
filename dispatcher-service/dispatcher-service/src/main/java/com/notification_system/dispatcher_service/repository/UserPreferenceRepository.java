package com.notification_system.dispatcher_service.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.notification_system.dispatcher_service.entity.UserPreference;
import com.notification_system.dispatcher_service.entity.UserPreference.UserPreferenceId;


public interface UserPreferenceRepository
        extends JpaRepository<UserPreference, UserPreferenceId> {

    Optional<UserPreference> findByUserIdAndChannel(String userId, String channel);
}