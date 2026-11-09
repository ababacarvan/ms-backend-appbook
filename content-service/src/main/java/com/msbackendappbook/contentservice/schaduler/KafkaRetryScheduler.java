package com.msbackendappbook.contentservice.schaduler;

import com.msbackendappbook.contentservice.service.KafkaEventRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRetryScheduler {

    private final KafkaEventRetryService kafkaEventRetryService;

    /**
     * Scheduler principal pour les retries - Toutes les 30 secondes
     */
    @Async
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void processRetries() {
        try {
            kafkaEventRetryService.processPendingRetries();
        } catch (Exception e) {
            log.error("Error in retry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler pour la maintenance - Toutes les heures
     */
    @Async
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void maintenance() {
        try {
            kafkaEventRetryService.markExpiredEvents();
        } catch (Exception e) {
            log.error("Error in maintenance scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler pour le nettoyage - Tous les jours à 3h
     */
    @Async
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        try {
            kafkaEventRetryService.cleanupOldEvents();
        } catch (Exception e) {
            log.error("Error in cleanup scheduler: {}", e.getMessage(), e);
        }
    }
}