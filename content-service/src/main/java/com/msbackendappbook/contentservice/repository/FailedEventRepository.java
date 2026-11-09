package com.msbackendappbook.contentservice.repository;

import com.msbackendappbook.contentservice.model.entity.FailedEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, UUID> {

    /**
     * Récupérer les événements prêts pour un retry
     */
    @Query("SELECT f FROM FailedEvent f WHERE f.status = 'PENDING' " +
            "AND f.retryCount < f.maxRetries " +
            "AND f.nextRetryAt <= :now " +
            "ORDER BY f.nextRetryAt ASC")
    List<FailedEvent> findPendingRetries(@Param("now") LocalDateTime now);

    /**
     * Récupérer les événements prêts pour un retry avec limite
     */
    @Query("SELECT f FROM FailedEvent f WHERE f.status = 'PENDING' " +
            "AND f.retryCount < f.maxRetries " +
            "AND f.nextRetryAt <= :now " +
            "ORDER BY f.nextRetryAt ASC")
    List<FailedEvent> findPendingRetries(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Récupérer les événements échoués par topic
     */
    List<FailedEvent> findByTopicAndStatus(String topic, FailedEvent.Status status);

    /**
     * Récupérer les événements expirés (plus de 4 jours)
     */
    @Query("SELECT f FROM FailedEvent f WHERE f.createdAt <= :expiryDate AND f.status != 'SUCCESS'")
    List<FailedEvent> findExpiredEvents(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Compter les événements en attente
     */
    long countByStatus(FailedEvent.Status status);

    /**
     * Compter les événements en attente par topic
     */
    long countByTopicAndStatus(String topic, FailedEvent.Status status);

    /**
     * Mettre à jour le statut d'un événement
     */
    @Modifying
    @Query("UPDATE FailedEvent f SET f.status = :status WHERE f.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") FailedEvent.Status status);

    /**
     * Marquer un événement comme réussi
     */
    @Modifying
    @Query("UPDATE FailedEvent f SET f.status = 'SUCCESS' WHERE f.id = :id")
    void markAsSuccess(@Param("id") UUID id);

    /**
     * Marquer un événement comme échoué définitivement
     */
    @Modifying
    @Query("UPDATE FailedEvent f SET f.status = 'FAILED' WHERE f.id = :id")
    void markAsFailed(@Param("id") UUID id);

    /**
     * Incrémenter le compteur de retry
     */
    @Modifying
    @Query("UPDATE FailedEvent f SET f.retryCount = f.retryCount + 1, " +
            "f.lastRetryAt = :now, f.nextRetryAt = :nextRetry " +
            "WHERE f.id = :id")
    void incrementRetry(@Param("id") UUID id,
                        @Param("now") LocalDateTime now,
                        @Param("nextRetry") LocalDateTime nextRetry);

    /**
     * Supprimer les événements réussis ou expirés
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FailedEvent f WHERE f.status IN ('SUCCESS', 'EXPIRED', 'FAILED') AND f.createdAt <= :cutoffDate")
    void deleteOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Récupérer les statistiques par topic
     */
    @Query("SELECT f.topic, f.status, COUNT(f) FROM FailedEvent f GROUP BY f.topic, f.status")
    List<Object[]> getStatisticsByTopic();
}