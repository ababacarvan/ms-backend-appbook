package com.msbackendappbook.userservice.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private UUID eventId;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String eventType; // USER_CREATED, USER_UPDATED, USER_DELETED, EMAIL_VERIFIED, USER_LOGIN
    private LocalDateTime timestamp;
}