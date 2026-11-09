package com.msbackendappbook.contentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbackendappbook.contentservice.model.entity.EventRetry;
import com.msbackendappbook.contentservice.model.entity.FailedEvent;
import com.msbackendappbook.contentservice.repository.EventRetryRepository;
import com.msbackendappbook.contentservice.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FailedEventRepository failedEventRepository;
    private final EventRetryRepository eventRetryRepository;

    private static final int RETRY_DELAY_SECONDS = 60; // 1 minute
    private static final int MAX_RETRIES = 5;
    private static final int KAFKA_TIMEOUT_SECONDS = 10;

    /**
     * Publier un événement avec gestion d'échec
     */
    @Async
    @Transactional
    public void publishEvent(String topic, String key, Object payload, Map<String, String> headers) {
        UUID eventId = UUID.randomUUID();

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Tenter d'envoyer à Kafka
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, payload);

            SendResult<String, Object> result = future.get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("✅ Event {} published to topic: {}, offset: {}",
                    eventId, topic, result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("❌ Failed to publish event {} to topic {}: {}", eventId, topic, e.getMessage());

            // Sauvegarder l'événement pour retry
            saveFailedEvent(eventId, topic, key, payload, headers, e);

            // Lancer le scheduler de retry immédiatement pour ce cas
            scheduleImmediateRetry(eventId);
        }
    }

    /**
     * Sauvegarder l'événement échoué
     */
    private void saveFailedEvent(UUID eventId, String topic, String key, Object payload,
                                 Map<String, String> headers, Exception error) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String headersJson = headers != null ? objectMapper.writeValueAsString(headers) : null;

            FailedEvent failedEvent = FailedEvent.builder()
                    .eventId(eventId)
                    .topic(topic)
                    .key(key)
                    .payload(payloadJson)
                    .headers(headersJson)
                    .errorMessage(error.getMessage())
                    .errorStackTrace(getStackTrace(error))
                    .status(FailedEvent.Status.PENDING)
                    .retryCount(0)
                    .maxRetries(MAX_RETRIES)
                    .nextRetryAt(LocalDateTime.now().plusSeconds(60)) // 1ère tentative dans 1 minute
                    .createdAt(LocalDateTime.now())
                    .build();

            failedEventRepository.save(failedEvent);
            log.info("Event {} saved for retry", eventId);

        } catch (Exception e) {
            log.error("Failed to save failed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Planifier un retry immédiat
     */
    @Async
    @Transactional
    public void scheduleImmediateRetry(UUID eventId) {
        try {
            Thread.sleep(1000); // Attendre 1 seconde avant de retry
            retryFailedEvent(eventId);
        } catch (Exception e) {
            log.error("Failed to schedule immediate retry for event {}: {}", eventId, e.getMessage());
        }
    }

    /**
     * Retry un événement échoué
     */
    @Transactional
    public boolean retryFailedEvent(UUID eventId) {
        FailedEvent failedEvent = failedEventRepository.findById(eventId).orElse(null);
        if (failedEvent == null) {
            log.warn("Failed event not found: {}", eventId);
            return false;
        }

        if (!failedEvent.isRetryable()) {
            log.warn("Event {} is not retryable. Status: {}, RetryCount: {}",
                    eventId, failedEvent.getStatus(), failedEvent.getRetryCount());
            return false;
        }

        long startTime = System.currentTimeMillis();

        try {
            // Marquer comme en cours de traitement
            failedEvent.setStatus(FailedEvent.Status.PROCESSING);
            failedEventRepository.save(failedEvent);

            // Tenter de republier
            String payloadJson = failedEvent.getPayload();
            Object payload = objectMapper.readValue(payloadJson, Object.class);

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(failedEvent.getTopic(), failedEvent.getKey(), payload);

            SendResult<String, Object> result = future.get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Succès
            failedEvent.setStatus(FailedEvent.Status.SUCCESS);
            failedEventRepository.save(failedEvent);

            // Log du succès
            saveRetryLog(failedEvent, startTime, EventRetry.Status.SUCCESS, null);

            log.info("✅ Event {} retry succeeded, offset: {}",
                    eventId, result.getRecordMetadata().offset());

            return true;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Incrémenter le compteur de retry
            failedEvent.incrementRetryCount();
            failedEvent.setErrorMessage(e.getMessage());
            failedEvent.setErrorStackTrace(getStackTrace(e));
            failedEventRepository.save(failedEvent);

            // Log de l'échec
            saveRetryLog(failedEvent, startTime, EventRetry.Status.FAILED, e.getMessage());

            log.warn("❌ Event {} retry {} failed: {}",
                    eventId, failedEvent.getRetryCount(), e.getMessage());

            return false;
        }
    }

    /**
     * Sauvegarder le log de retry
     */
    private void saveRetryLog(FailedEvent failedEvent, long startTime, EventRetry.Status status, String errorMessage) {
        try {
            EventRetry retryLog = EventRetry.builder()
                    .failedEventId(failedEvent.getId())
                    .attemptNumber(failedEvent.getRetryCount())
                    .status(status)
                    .errorMessage(errorMessage)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .retryDelaySeconds(60L * (long) Math.pow(2, failedEvent.getRetryCount() - 1))
                    .createdAt(LocalDateTime.now())
                    .build();

            eventRetryRepository.save(retryLog);
        } catch (Exception e) {
            log.error("Failed to save retry log: {}", e.getMessage());
        }
    }

    /**
     * Récupérer la stacktrace
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 5000) break; // Limiter la taille
        }
        return sb.toString();
    }
}