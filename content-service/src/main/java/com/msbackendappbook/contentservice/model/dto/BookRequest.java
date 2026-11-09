package com.msbackendappbook.contentservice.model.dto;

import com.msbackendappbook.contentservice.model.entity.Book;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;

    private String description;
    @NotNull(message = "ISBN is required")
    private String isbn;

    @NotNull(message = "Author ID is required")
    private UUID authorId;

    private String publisher;

    private LocalDate publicationDate;

    private String language;

    private Book.Format format;

    private Integer pagesCount;

    private Set<UUID> categoryIds;

    private Set<UUID> tagIds;

    private boolean featured;

    private boolean available;

    private boolean approved;
}