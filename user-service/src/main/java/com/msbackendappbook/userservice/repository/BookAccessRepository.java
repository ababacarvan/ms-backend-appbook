package com.msbackendappbook.userservice.repository;

import com.msbackendappbook.userservice.model.entity.BookAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookAccessRepository extends JpaRepository<BookAccessLog, UUID> {

    @Query("SELECT COUNT(b) > 0 FROM BookAccessLog b WHERE b.userId = :userId AND b.bookId = :bookId AND b.accessType = 'PREVIEW'")
    boolean existsByUserIdAndBookId(@Param("userId") UUID userId, @Param("bookId") UUID bookId);

    long countByUserIdAndBookIdAndAccessType(UUID userId, UUID bookId, String accessType);
}