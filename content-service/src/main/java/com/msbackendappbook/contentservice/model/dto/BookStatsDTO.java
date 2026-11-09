package com.msbackendappbook.contentservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookStatsDTO {
    private long totalBooks;
    private long approvedBooks;
    private long pendingBooks;
    private long featuredBooks;
    private long totalDownloads;
    private long totalViews;
    private Map<String, Long> booksByFormat;
    private Map<String, Long> booksByLanguage;
}