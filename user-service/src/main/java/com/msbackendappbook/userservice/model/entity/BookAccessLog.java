package com.msbackendappbook.userservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "book_access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "access_type", length = 20)
    private String accessType; // FULL, PREVIEW

    @Column(name = "pages_allowed")
    private int pagesAllowed;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}