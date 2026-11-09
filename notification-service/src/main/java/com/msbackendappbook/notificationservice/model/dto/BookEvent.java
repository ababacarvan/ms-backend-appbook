package com.msbackendappbook.notificationservice.model.dto;

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
public class BookEvent {
    private UUID eventId;
    private UUID bookId;
    private UUID authorId;
    private String authorName;
    private String title;
    private String eventType; // BOOK_CREATED, BOOK_APPROVED, BOOK_DOWNLOADED
    private LocalDateTime timestamp;
}