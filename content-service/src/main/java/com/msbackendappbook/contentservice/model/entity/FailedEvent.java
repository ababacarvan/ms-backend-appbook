package com.msbackendappbook.contentservice.model.entity;

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
@Table(name = "failed_events", indexes = {
        @Index(name = "idx_failed_events_status", columnList = "status"),
        @Index(name = "idx_failed_events_created_at", columnList = "createdAt"),
        @Index(name = "idx_failed_events_next_retry", columnList = "nextRetryAt"),
        @Index(name = "idx_failed_events_topic", columnList = "topic")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "key", length = 255)
    private String key;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stacktrace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private int maxRetries = 5;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,      // En attente de retry
        PROCESSING,   // En cours de traitement
        SUCCESS,      // Traité avec succès
        FAILED,       // Échec définitif
        EXPIRED,      // Expiré (max retries atteint)
        CANCELLED     // Annulé
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();

        if (this.retryCount >= this.maxRetries) {
            this.status = Status.FAILED;
        } else {
            // Calculer le prochain délai (exponentiel)
            long delayInSeconds = (long) Math.pow(2, this.retryCount) * 60; // 1min, 2min, 4min, 8min, 16min
            this.nextRetryAt = LocalDateTime.now().plusSeconds(delayInSeconds);
        }
    }

    public boolean isRetryable() {
        return this.status == Status.PENDING &&
                this.retryCount < this.maxRetries &&
                this.nextRetryAt != null &&
                this.nextRetryAt.isBefore(LocalDateTime.now());
    }

    public boolean isExpired() {
        return this.retryCount >= this.maxRetries ||
                (this.createdAt != null &&
                        this.createdAt.plusDays(4).isBefore(LocalDateTime.now()));
    }
}