package com.msbackendappbook.userservice.repository;

import com.msbackendappbook.userservice.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false AND o.createdAt <= :before ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsBefore(@Param("before") LocalDateTime before);

    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsWithRetryCountLessThan(@Param("maxRetries") int maxRetries);

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.published = true, o.publishedAt = :publishedAt WHERE o.id = :id")
    void markAsPublished(@Param("id") UUID id, @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.retryCount = o.retryCount + 1, o.lastError = :error WHERE o.id = :id")
    void incrementRetryCount(@Param("id") UUID id, @Param("error") String error);

    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEvent o WHERE o.published = true AND o.createdAt <= :before")
    void deletePublishedEventsOlderThan(@Param("before") LocalDateTime before);

    Optional<OutboxEvent> findByEventId(UUID eventId);

    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.published = false AND o.createdAt < :timeout")
    long countUnpublishedEventsWithTimeout(@Param("timeout") LocalDateTime timeout);
}