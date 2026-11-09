package com.msbackendappbook.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbackendappbook.notificationservice.consumer.KafkaEventConsumer;
import com.msbackendappbook.notificationservice.model.entity.FailedEvent;
import com.msbackendappbook.notificationservice.repository.FailedEventRepository;
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
public class EventRetryService {

    private final FailedEventRepository failedEventRepository;
    private final KafkaEventConsumer kafkaEventConsumer;
    private final ObjectMapper objectMapper;

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
                    // Marquer comme en cours
                    event.setStatus(FailedEvent.Status.PROCESSING);
                    failedEventRepository.save(event);

                    // Rejouer l'événement
                    switch (event.getTopic()) {
                        case "user-events":
                            kafkaEventConsumer.consumeUserEvent(event.getPayload(), null);
                            break;
                        case "book-events":
                            kafkaEventConsumer.consumeBookEvent(event.getPayload(), null);
                            break;
                        case "payment-events":
                            kafkaEventConsumer.consumePaymentEvent(event.getPayload(), null);
                            break;
                        default:
                            log.warn("Unknown topic: {}", event.getTopic());
                    }

                    // Marquer comme succès
                    event.setStatus(FailedEvent.Status.SUCCESS);
                    failedEventRepository.save(event);
                    log.info("✅ Failed event {} retry succeeded", event.getId());

                } catch (Exception e) {
                    log.error("❌ Retry failed for event {}: {}", event.getId(), e.getMessage());

                    event.setRetryCount(event.getRetryCount() + 1);
                    event.setErrorMessage(e.getMessage());

                    if (event.getRetryCount() >= event.getMaxRetries()) {
                        event.setStatus(FailedEvent.Status.FAILED);
                        log.warn("⚠️ Event {} reached max retries", event.getId());
                    } else {
                        event.setNextRetryAt(LocalDateTime.now().plusMinutes(
                                (long) Math.pow(2, event.getRetryCount()) * 5
                        ));
                    }

                    failedEventRepository.save(event);
                }
            }

        } catch (Exception e) {
            log.error("Error in retry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Nettoyer les vieux événements - Tous les jours
     */
    @Scheduled(cron = "0 0 3 * * ?")
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
}