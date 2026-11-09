package com.msbackendappbook.notificationservice.repository;


import com.msbackendappbook.notificationservice.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatus(UUID userId, Notification.Status status, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' OR n.status = 'RETRYING'")
    List<Notification> findPendingNotifications();

    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 5 AND n.createdAt > :since")
    List<Notification> findRetryableNotifications(@Param("since") LocalDateTime since);

    long countByStatus(Notification.Status status);

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.status = 'SENT' AND n.createdAt > :since GROUP BY n.type")
    List<Object[]> countByType(@Param("since") LocalDateTime since);
}