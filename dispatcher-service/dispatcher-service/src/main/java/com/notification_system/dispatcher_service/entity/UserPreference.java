package com.notification_system.dispatcher_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "user_preferences")
@IdClass(UserPreference.UserPreferenceId.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "channel")
    private String channel;

    @Column(name = "opted_in")
    private Boolean optedIn;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class UserPreferenceId implements java.io.Serializable {
        private String userId;
        private String channel;
    }
}