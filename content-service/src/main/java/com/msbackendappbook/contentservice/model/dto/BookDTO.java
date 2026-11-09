package com.msbackendappbook.contentservice.model.dto;

import com.msbackendappbook.contentservice.model.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private UUID id;
    private String title;
    private String subtitle;
    private String description;
    private String isbn;
    private AuthorDTO author;
    private String slug;
    private String publisher;
    private LocalDate publicationDate;
    private String language;
    private String coverImageUrl;
    private String coverImageName;
    private Long coverImageSize;
    private String fileContentType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Book.Format format;
    private Integer pagesCount;
    private Double averageRating;
    private Integer downloadCount;
    private Integer viewCount;
    private boolean featured;
    private boolean available;
    private boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<CategoryDTO> categories;
    private Set<TagDTO> tags;
}