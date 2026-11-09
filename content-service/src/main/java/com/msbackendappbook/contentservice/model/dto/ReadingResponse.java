package com.msbackendappbook.contentservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingResponse {
    private UUID sessionId;
    private UUID bookId;
    private UUID userId;
    private String bookTitle;
    private String bookContent;
    private boolean hasFullAccess;
    private int accessPercentage;
    private int currentPage;
    private int totalPages;
    private double percentageRead;
    private String planType;
    private String message;
}