package com.msbackendappbook.userservice.repository;

import com.msbackendappbook.userservice.model.entity.ReadingProgress;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, UUID> {
    Optional<ReadingProgress> findByUserAndBookId(User user, UUID bookId);
    List<ReadingProgress> findByUserAndStatus(User user, Status status);

    @Modifying
    @Query("""
           UPDATE ReadingProgress rp
           SET rp.currentPage = :page,
               rp.percentageRead = :percentage,
               rp.lastReadAt = CURRENT_TIMESTAMP,
               rp.status = :status
           WHERE rp.id = :id
           """)
    int updateProgress(
            @Param("id") UUID id,
            @Param("page") Integer page,
            @Param("percentage") Double percentage,
            @Param("status") Status status
    );
}
