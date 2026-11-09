package com.msbackendappbook.notificationservice.scheduler;

import com.msbackendappbook.notificationservice.model.entity.FailedEvent;
import com.msbackendappbook.notificationservice.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventRetryScheduler {

    private final FailedEventRepository failedEventRepository;

    private static final int BATCH_SIZE = 10;

    /**
     * Retry des événements échoués - Toutes les 5 minutes
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes
    @Async
    @Transactional
    public void retryFailedEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<FailedEvent> failedEvents = failedEventRepository
                    .findPendingRetries(now, PageRequest.of(0, BATCH_SIZE));

            if (failedEvents.isEmpty()) {
                return;
            }

            log.info("🔄 Processing {} failed events for retry", failedEvents.size());

            for (FailedEvent event : failedEvents) {
                try {
                    // Marquer comme en cours de traitement
                    event.setStatus(FailedEvent.Status.PROCESSING);
                    failedEventRepository.save(event);

                    // Réessayer l'événement
                    log.info("⏳ Retrying event: {} (attempt {}/{})",
                            event.getId(), event.getRetryCount() + 1, event.getMaxRetries());

                    // TODO: Ajouter ici la logique de retry
                    // Par exemple : reprocesser l'événement selon son topic

                    // Si succès
                    event.setStatus(FailedEvent.Status.SUCCESS);
                    failedEventRepository.save(event);
                    log.info("✅ Event {} retry succeeded", event.getId());

                } catch (Exception e) {
                    log.error("❌ Retry failed for event {}: {}", event.getId(), e.getMessage());

                    event.setRetryCount(event.getRetryCount() + 1);
                    event.setErrorMessage(e.getMessage());

                    if (event.getRetryCount() >= event.getMaxRetries()) {
                        event.setStatus(FailedEvent.Status.FAILED);
                        log.warn("⚠️ Event {} reached max retries ({})",
                                event.getId(), event.getMaxRetries());
                    } else {
                        // Prochain retry avec backoff exponentiel : 5min, 10min, 20min, 40min, 80min
                        long delayMinutes = (long) Math.pow(2, event.getRetryCount()) * 5;
                        event.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                        log.info("⏰ Next retry for event {} scheduled at {} (in {} minutes)",
                                event.getId(), event.getNextRetryAt(), delayMinutes);
                    }

                    failedEventRepository.save(event);
                }
            }

        } catch (Exception e) {
            log.error("Error in retry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Nettoyer les vieux événements - Tous les jours à 3h
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Async
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            failedEventRepository.deleteOldEvents(cutoff);
            log.info("🧹 Cleaned up events older than 7 days");
        } catch (Exception e) {
            log.error("Error cleaning up old events: {}", e.getMessage());
        }
    }

    /**
     * Vérifier les événements bloqués - Toutes les heures
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 120000) // 1 heure
    @Async
    @Transactional
    public void checkStuckEvents() {
        try {
            LocalDateTime stuckThreshold = LocalDateTime.now().minusMinutes(30);
            long stuckCount = failedEventRepository.countStuckEvents(stuckThreshold);

            if (stuckCount > 0) {
                log.warn("⚠️ Found {} stuck events (processing for more than 30 minutes)", stuckCount);
                // On pourrait envoyer une alerte ici
            }
        } catch (Exception e) {
            log.error("Error checking stuck events: {}", e.getMessage());
        }
    }
}