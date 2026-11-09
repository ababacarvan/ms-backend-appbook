package com.msbackendappbook.contentservice.service;

import com.msbackendappbook.contentservice.model.entity.FailedEvent;
import com.msbackendappbook.contentservice.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventRetryService {

    private final FailedEventRepository failedEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRY_PROCESSING = 100;

    /**
     * Scheduler pour retry des événements échoués - Toutes les 30 secondes
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    @Async
    @Transactional
    public void processPendingRetries() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<FailedEvent> pendingEvents = failedEventRepository.findPendingRetries(now, PageRequest.of(0, BATCH_SIZE));

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("📬 Found {} pending events to retry", pendingEvents.size());

            int processed = 0;
            for (FailedEvent event : pendingEvents) {
                if (processed >= MAX_RETRY_PROCESSING) {
                    break;
                }

                try {
                    kafkaEventPublisher.retryFailedEvent(event.getId());
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to retry event {}: {}", event.getId(), e.getMessage());
                }
            }

            log.info("✅ Processed {} retries, remaining: {}", processed, pendingEvents.size() - processed);

        } catch (Exception e) {
            log.error("Error in retry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler pour marquer les événements expirés - Toutes les heures
     */
    @Scheduled(fixedDelay = 3600000) // 1 heure
    @Transactional
    public void markExpiredEvents() {
        try {
            LocalDateTime expiryDate = LocalDateTime.now().minusDays(4);
            List<FailedEvent> expiredEvents = failedEventRepository.findExpiredEvents(expiryDate);

            if (expiredEvents.isEmpty()) {
                return;
            }

            int expiredCount = 0;
            for (FailedEvent event : expiredEvents) {
                if (event.getStatus() != FailedEvent.Status.SUCCESS) {
                    event.setStatus(FailedEvent.Status.EXPIRED);
                    event.setErrorMessage("Event expired after 4 days");
                    failedEventRepository.save(event);
                    expiredCount++;
                }
            }

            if (expiredCount > 0) {
                log.warn("⚠️ Marked {} events as expired (older than 4 days)", expiredCount);
            }

        } catch (Exception e) {
            log.error("Error marking expired events: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler pour nettoyer les vieux événements - Tous les jours à 3h
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            failedEventRepository.deleteOldEvents(cutoffDate);
            log.info("🧹 Cleaned up events older than 7 days");

        } catch (Exception e) {
            log.error("Error cleaning up old events: {}", e.getMessage(), e);
        }
    }
}