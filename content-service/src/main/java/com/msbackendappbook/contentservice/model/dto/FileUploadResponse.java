package com.msbackendappbook.contentservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String message;
    private boolean success;
}