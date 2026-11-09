package com.msbackendappbook.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbackendappbook.userservice.model.entity.OutboxEvent;
import com.msbackendappbook.userservice.repository.OutboxEventRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    @Transactional
    public void publishEvents() {
        // Récupérer uniquement les événements avec retryCount < MAX_RETRIES
        List<OutboxEvent> events = outboxEventRepository
                .findUnpublishedEventsWithRetryCountLessThan(MAX_RETRIES);

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unpublished events to process", events.size());

        // Traiter par lots
        for (int i = 0; i < events.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, events.size());
            List<OutboxEvent> batch = events.subList(i, end);
            processBatch(batch);
        }
    }

    @Async
    public void processBatch(List<OutboxEvent> events) {
        events.forEach(this::publishEvent);
    }

    @Retry(name = "kafkaPublisher", fallbackMethod = "fallbackPublish")
    public void publishEvent(OutboxEvent event) {
        try {
            String topic = getTopicForEventType(event.getEventType());

            // Envoyer avec key pour garantir l'ordre des événements pour un même aggregate
            String key = event.getAggregateId() != null ?
                    event.getAggregateId().toString() :
                    event.getEventId().toString();

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(topic, key, event.getPayload());

            // Attendre le résultat avec timeout
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);

            // Marquer comme publié
            markAsPublished(event.getId());
            log.info("Event {} successfully published to topic: {}", event.getEventId(), topic);

        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getEventId(), ex.getMessage());
            throw new RuntimeException("Kafka publish failed", ex);
        }
    }

    public void fallbackPublish(OutboxEvent event, Exception ex) {
        log.warn("Kafka unavailable for event {}, will retry later", event.getEventId());
        // Incrémenter le compteur de retry
        try {
            outboxEventRepository.incrementRetryCount(event.getId(),
                    "Kafka unavailable: " + ex.getMessage());
        } catch (Exception e) {
            log.error("Failed to update retry count for event: {}", event.getId(), e);
        }
    }

    private String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "USER_CREATED", "USER_UPDATED", "USER_DELETED", "EMAIL_VERIFIED", "USER_LOGIN" ->
                    "user-events";
            case "PAYMENT_SUCCESS", "PAYMENT_FAILED" ->
                    "payment-events";
            case "SUBSCRIPTION_CREATED", "SUBSCRIPTION_EXPIRED" ->
                    "subscription-events";
            default -> {
                log.warn("Unknown event type: {}, using default topic", eventType);
                yield "default-events";
            }
        };
    }

    @Transactional
    public void markAsPublished(UUID eventId) {
        outboxEventRepository.markAsPublished(eventId, LocalDateTime.now());
        log.debug("Event {} marked as published", eventId);
    }

    /**
     * Nettoyage des anciens événements publiés
     */
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        outboxEventRepository.deletePublishedEventsOlderThan(cutoff);
        log.info("Cleaned up outbox events older than: {}", cutoff);
    }
}