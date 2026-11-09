package com.msbackendappbook.userservice.model.dto;

import com.msbackendappbook.userservice.model.enums.Status;
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
public class ReadingProgressDTO {
    private UUID id;
    private UUID userId;
    private UUID bookId;
    private int currentPage;
    private int totalPages;
    private double percentageRead;
    private Status status;
    private LocalDateTime lastReadAt;
    private boolean hasSubscription;
    private int maxReadablePages;
    private boolean isCompleted;
}