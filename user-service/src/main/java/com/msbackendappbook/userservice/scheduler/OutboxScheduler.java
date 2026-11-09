package com.msbackendappbook.userservice.scheduler;

import com.msbackendappbook.userservice.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxPublisherService outboxPublisherService;

    @Async
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishEvents() {
        try {
            outboxPublisherService.publishEvents();
        } catch (Exception e) {
            log.error("Error in outbox scheduler: {}", e.getMessage(), e);
        }
    }

    @Async
    @Scheduled(cron = "0 0 3 * * ?") // Tous les jours à 3h du matin
    public void cleanupOldEvents() {
        try {
            outboxPublisherService.cleanupOldEvents();
            log.info("Outbox cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error in outbox cleanup: {}", e.getMessage(), e);
        }
    }
}