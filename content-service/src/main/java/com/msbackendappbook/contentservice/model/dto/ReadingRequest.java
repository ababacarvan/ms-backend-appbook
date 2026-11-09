package com.msbackendappbook.contentservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingRequest {

    @NotNull(message = "Book ID is required")
    private UUID bookId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private Integer pageNumber = 1;

    private String deviceInfo;

    private String ipAddress;
}