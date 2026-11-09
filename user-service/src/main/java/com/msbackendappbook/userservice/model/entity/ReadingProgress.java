package com.msbackendappbook.userservice.model.entity;

import com.msbackendappbook.userservice.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reading_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "current_page")
    @Builder.Default
    private int currentPage = 0;

    @Column(name = "total_pages")
    private int totalPages;

    @Column(name = "percentage_read")
    @Builder.Default
    private double percentageRead = 0.0;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.NOT_STARTED;

    @Column(name = "has_subscription")
    @Builder.Default
    private boolean hasSubscription = false;

    @Column(name = "max_readable_pages")
    @Builder.Default
    private int maxReadablePages = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}