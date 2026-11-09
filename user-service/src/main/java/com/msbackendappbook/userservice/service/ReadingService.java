package com.msbackendappbook.userservice.service;

import com.msbackendappbook.userservice.model.dto.BookAccess;
import com.msbackendappbook.userservice.model.entity.BookAccessLog;
import com.msbackendappbook.userservice.model.entity.ReadingProgress;
import com.msbackendappbook.userservice.model.entity.Subscription;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.model.enums.PlanType;
import com.msbackendappbook.userservice.model.enums.Status;
import com.msbackendappbook.userservice.repository.BookAccessRepository;
import com.msbackendappbook.userservice.repository.ReadingProgressRepository;
import com.msbackendappbook.userservice.repository.SubscriptionRepository;
import com.msbackendappbook.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingService {

    private static final int PREVIEW_PAGES = 10;
    private static final double PREVIEW_PERCENTAGE = 0.02;

    private final ReadingProgressRepository readingProgressRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BookAccessRepository bookAccessRepository;
    private final UserRepository userRepository;

    /**
     * Vérifier si un utilisateur peut lire un livre
     */
    public BookAccess checkBookAccess(UUID userId, UUID bookId, int totalPages) {
        log.info("🔍 checkBookAccess - userId: {}, bookId: {}, totalPages: {}", userId, bookId, totalPages);

        // ✅ 1. Si userId est null, utilisateur non connecté
        if (userId == null) {
            log.warn("❌ userId is null - User not authenticated");
            return BookAccess.builder()
                    .canRead(false)
                    .reason("Vous devez être connecté pour lire ce livre")
                    .maxPages(0)
                    .isSubscriber(false)
                    .build();
        }

        // ✅ 2. Récupérer l'utilisateur
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("❌ User not found: {}", userId);
            return BookAccess.builder()
                    .canRead(false)
                    .reason("Utilisateur non trouvé")
                    .maxPages(0)
                    .isSubscriber(false)
                    .build();
        }

        User user = userOpt.get();
        log.info("✅ User found: {}", user.getEmail());

        // ✅ 3. Vérifier si l'utilisateur a un abonnement actif
        Optional<Subscription> subscription = subscriptionRepository.findActiveSubscriptionByUser(user);

        if (subscription.isPresent() && subscription.get().isActive()) {
            log.info("✅ User has active subscription: {}", subscription.get().getPlanType());
            return BookAccess.builder()
                    .canRead(true)
                    .reason("Accès complet - Abonné")
                    .maxPages(totalPages)
                    .isSubscriber(true)
                    .subscriptionType(subscription.get().getPlanType())
                    .build();
        }

        // ✅ 4. Vérifier si l'utilisateur a un essai gratuit
        Optional<Subscription> trial = subscriptionRepository.findActiveSubscriptionByUser(user)
                .filter(s -> s.getPlanType() == PlanType.FREE_TRIAL);

        if (trial.isPresent() && trial.get().isActive()) {
            log.info("✅ User has free trial");
            return BookAccess.builder()
                    .canRead(true)
                    .reason("Période d'essai gratuit")
                    .maxPages(totalPages)
                    .isSubscriber(true)
                    .subscriptionType(PlanType.FREE_TRIAL)
                    .build();
        }

        // ✅ 5. Utilisateur non abonné → Preview 2%
        int previewPages = Math.max(1, (int) (totalPages * PREVIEW_PERCENTAGE));
        previewPages = Math.min(previewPages, PREVIEW_PAGES);

        log.info("📖 User not subscribed - Preview: {} pages", previewPages);

        // Vérifier si l'utilisateur a déjà utilisé sa preview
        boolean hasUsedPreview = bookAccessRepository.existsByUserIdAndBookId(userId, bookId);

        if (hasUsedPreview) {
            log.info("⚠️ User already used preview for book {}", bookId);
            return BookAccess.builder()
                    .canRead(false)
                    .reason("Vous avez déjà utilisé votre preview pour ce livre")
                    .maxPages(0)
                    .hasUsedPreview(true)
                    .isSubscriber(false)
                    .build();
        }

        return BookAccess.builder()
                .canRead(true)
                .reason(String.format("Preview - %d%% du contenu (max %d pages)",
                        (int) (PREVIEW_PERCENTAGE * 100), previewPages))
                .maxPages(previewPages)
                .isSubscriber(false)
                .hasUsedPreview(false)
                .previewPages(previewPages)
                .build();
    }

    /**
     * Démarrer la lecture d'un livre
     */
    @Transactional
    public ReadingProgress startReading(UUID userId, UUID bookId, int totalPages) {
        log.info("📖 startReading - userId: {}, bookId: {}, totalPages: {}", userId, bookId, totalPages);

        BookAccess access = checkBookAccess(userId, bookId, totalPages);

        if (!access.isCanRead()) {
            throw new RuntimeException("Accès refusé: " + access.getReason());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ReadingProgress progress = readingProgressRepository
                .findByUserAndBookId(user, bookId)
                .orElse(ReadingProgress.builder()
                        .user(user)
                        .bookId(bookId)
                        .totalPages(totalPages)
                        .currentPage(0)
                        .percentageRead(0.0)
                        .status(Status.NOT_STARTED)
                        .maxReadablePages(access.getMaxPages())
                        .hasSubscription(access.isSubscriber())
                        .lastReadAt(LocalDateTime.now())
                        .build());

        // Si non abonné, limiter aux pages de preview
        if (!access.isSubscriber()) {
            progress.setMaxReadablePages(access.getMaxPages());
        }

        progress = readingProgressRepository.save(progress);

        // Log l'accès
        logAccess(userId, bookId, access);

        log.info("✅ User {} started reading book {}", userId, bookId);
        return progress;
    }

    /**
     * Mettre à jour la progression de lecture
     */
    @Transactional
    public ReadingProgress updateProgress(UUID userId, UUID bookId, int currentPage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ReadingProgress progress = readingProgressRepository
                .findByUserAndBookId(user, bookId)
                .orElseThrow(() -> new RuntimeException("Reading progress not found"));

        // Vérifier la limite pour les non-abonnés
        if (!progress.isHasSubscription()) {
            int maxPages = progress.getMaxReadablePages();
            if (currentPage > maxPages) {
                throw new RuntimeException("Limite de preview atteinte. Abonnez-vous pour continuer.");
            }
        }

        progress.setCurrentPage(currentPage);
        progress.setPercentageRead((currentPage / (double) progress.getTotalPages()) * 100);

        if (currentPage >= progress.getTotalPages()) {
            progress.setStatus(Status.COMPLETED);
        } else if (currentPage > 0) {
            progress.setStatus(Status.READING);
        }

        progress.setLastReadAt(LocalDateTime.now());

        log.debug("📖 User {} progress: {}/{} pages", userId, currentPage, progress.getTotalPages());
        return readingProgressRepository.save(progress);
    }

    /**
     * Obtenir la progression de lecture
     */
    public ReadingProgress getProgress(UUID userId, UUID bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return readingProgressRepository.findByUserAndBookId(user, bookId)
                .orElse(null);
    }

    /**
     * Journaliser l'accès au livre
     */
    private void logAccess(UUID userId, UUID bookId, BookAccess access) {
        try {
            BookAccessLog log = BookAccessLog.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .accessType(access.isSubscriber() ? "FULL" : "PREVIEW")
                    .pagesAllowed(access.getMaxPages())
                    .timestamp(LocalDateTime.now())
                    .build();
            bookAccessRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to log access: {}", e.getMessage());
        }
    }
}