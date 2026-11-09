package com.msbackendappbook.contentservice.repository;


import com.msbackendappbook.contentservice.model.entity.EventRetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRetryRepository extends JpaRepository<EventRetry, UUID> {

    /**
     * Récupérer tous les logs de retry pour un événement
     */
    Page<EventRetry> findByFailedEventIdOrderByCreatedAtDesc(UUID failedEventId, Pageable pageable);

    /**
     * Compter les tentatives pour un événement
     */
    long countByFailedEventId(UUID failedEventId);

    /**
     * Récupérer les derniers retry pour un événement
     */
    @Query("SELECT er FROM EventRetry er WHERE er.failedEventId = :failedEventId ORDER BY er.createdAt DESC")
    Page<EventRetry> findLatestRetries(@Param("failedEventId") UUID failedEventId, Pageable pageable);
}