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
public class CategoryDTO {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private UUID parentId;
    private String iconUrl;
}