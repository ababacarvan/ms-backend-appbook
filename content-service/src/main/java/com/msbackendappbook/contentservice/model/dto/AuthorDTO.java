package com.msbackendappbook.contentservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String biography;
    private String country;
    private String profilePictureUrl;
    private String email;
    private String website;
    private String slug;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}