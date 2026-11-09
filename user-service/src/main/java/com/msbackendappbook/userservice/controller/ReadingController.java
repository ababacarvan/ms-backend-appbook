package com.msbackendappbook.userservice.controller;

import com.msbackendappbook.userservice.model.dto.BookAccess;
import com.msbackendappbook.userservice.model.dto.ReadingProgressDTO;
import com.msbackendappbook.userservice.model.entity.ReadingProgress;
import com.msbackendappbook.userservice.model.enums.Status;
import com.msbackendappbook.userservice.service.ReadingService;
import com.msbackendappbook.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
@Slf4j
public class ReadingController {

    private final ReadingService readingService;
    private final UserService userService;  // ✅ Injection du UserService

    /**
     * Vérifier l'accès à un livre
     * GET /api/reading/{bookId}/access?totalPages=200
     */
    @GetMapping("/{bookId}/access")
    public ResponseEntity<BookAccess> checkAccess(
            @PathVariable UUID bookId,
            @RequestParam int totalPages) {

        UUID userId = getCurrentUserId();
        log.info("📖 checkAccess - userId: {}, bookId: {}, totalPages: {}", userId, bookId, totalPages);

        BookAccess access = readingService.checkBookAccess(userId, bookId, totalPages);
        log.info("📖 checkAccess result: {}", access);
        return ResponseEntity.ok(access);
    }

    /**
     * Démarrer la lecture
     * POST /api/reading/{bookId}/start?totalPages=200
     */
    @PostMapping("/{bookId}/start")
    public ResponseEntity<ReadingProgressDTO> startReading(
            @PathVariable UUID bookId,
            @RequestParam int totalPages) {

        UUID userId = getCurrentUserId();
        log.info("📖 startReading - userId: {}, bookId: {}, totalPages: {}", userId, bookId, totalPages);

        if (userId == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        ReadingProgress progress = readingService.startReading(userId, bookId, totalPages);
        return ResponseEntity.ok(mapToDTO(progress));
    }

    /**
     * Mettre à jour la progression
     * PUT /api/reading/{bookId}/progress?currentPage=50
     */
    @PutMapping("/{bookId}/progress")
    public ResponseEntity<ReadingProgressDTO> updateProgress(
            @PathVariable UUID bookId,
            @RequestParam int currentPage) {

        UUID userId = getCurrentUserId();
        log.info("📖 updateProgress - userId: {}, bookId: {}, currentPage: {}", userId, bookId, currentPage);

        if (userId == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        ReadingProgress progress = readingService.updateProgress(userId, bookId, currentPage);
        return ResponseEntity.ok(mapToDTO(progress));
    }

    /**
     * Obtenir la progression
     * GET /api/reading/{bookId}/progress
     */
    @GetMapping("/{bookId}/progress")
    public ResponseEntity<ReadingProgressDTO> getProgress(@PathVariable UUID bookId) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        ReadingProgress progress = readingService.getProgress(userId, bookId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDTO(progress));
    }

    /**
     * Récupérer l'ID de l'utilisateur connecté
     */
    private UUID getCurrentUserId() {
        try {
            // ✅ Utiliser le UserService pour récupérer l'utilisateur connecté
            return userService.getCurrentUserId();
        } catch (Exception e) {
            log.error("❌ Error getting current user ID: {}", e.getMessage());
            return null;
        }
    }

    private ReadingProgressDTO mapToDTO(ReadingProgress progress) {
        return ReadingProgressDTO.builder()
                .id(progress.getId())
                .userId(progress.getUser().getId())
                .bookId(progress.getBookId())
                .currentPage(progress.getCurrentPage())
                .totalPages(progress.getTotalPages())
                .percentageRead(progress.getPercentageRead())
                .status(progress.getStatus())
                .lastReadAt(progress.getLastReadAt())
                .hasSubscription(progress.isHasSubscription())
                .maxReadablePages(progress.getMaxReadablePages())
                .isCompleted(progress.getStatus() == Status.COMPLETED)
                .build();
    }
}