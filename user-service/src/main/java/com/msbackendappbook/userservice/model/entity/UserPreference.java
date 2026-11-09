package com.msbackendappbook.userservice.model.entity;

import com.msbackendappbook.userservice.model.enums.Theme;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity @AllArgsConstructor @NoArgsConstructor @Data @Builder
public class UserPreference {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String language;
    @Column(name = "font_size")
    private Integer fontSize;

    @Enumerated(EnumType.STRING)
    private Theme theme;

    @Column(name = "notification_enabled")
    private boolean notificationEnabled;

    @Column(name = "email_notifications")
    private boolean emailNotifications;
}
