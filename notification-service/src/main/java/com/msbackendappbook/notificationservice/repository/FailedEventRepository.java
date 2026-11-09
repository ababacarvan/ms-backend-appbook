package com.msbackendappbook.notificationservice.repository;

import com.msbackendappbook.notificationservice.model.entity.FailedEvent;
import com.msbackendappbook.notificationservice.model.entity.FailedEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FailedEventRepository extends JpaRepository<com.msbackendappbook.notificationservice.model.entity.FailedEvent, UUID> {

    /**
     * Récupérer les événements prêts pour un retry
     */
    @Query("SELECT f FROM FailedEvent f WHERE f.status = 'PENDING' AND f.nextRetryAt <= :now ORDER BY f.nextRetryAt ASC")
    List<FailedEvent> findPendingRetries(@Param("now") LocalDateTime now, Pageable pageable);

    // Compter les événements bloqués (en processing depuis trop longtemps)
    @Query("SELECT COUNT(f) FROM FailedEvent f WHERE f.status = 'PROCESSING' AND f.updatedAt <= :threshold")
    long countStuckEvents(@Param("threshold") LocalDateTime threshold);
    /**
     * Supprimer les événements anciens (SUCCESS, FAILED, EXPIRED)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FailedEvent f WHERE f.status IN ('SUCCESS', 'FAILED', 'EXPIRED') AND f.createdAt <= :cutoff")
    void deleteOldEvents(@Param("cutoff") LocalDateTime cutoff);  // ⬅️ Méthode ajoutée
}