package com.msbackendappbook.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbackendappbook.notificationservice.model.dto.BookEvent;
import com.msbackendappbook.notificationservice.model.dto.PaymentEvent;
import com.msbackendappbook.notificationservice.model.dto.UserEvent;
import com.msbackendappbook.notificationservice.service.BrevoEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final BrevoEmailService brevoEmailService;  // ⬅️ Injection du service
    private final ObjectMapper objectMapper;

    /**
     * Consommer les événements utilisateur
     */
    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void consumeUserEvent(String payload, Acknowledgment acknowledgment) {
        try {
            UserEvent event = objectMapper.readValue(payload, UserEvent.class);
            log.info("📨 Received user event: {} - {} (userId: {})",
                    event.getEventType(), event.getEmail(), event.getUserId());

            switch (event.getEventType()) {
                case "USER_CREATED":
                    // ✅ Passer l'userId
                    brevoEmailService.sendWelcomeEmail(
                            event.getEmail(),
                            event.getFirstName(),
                            event.getLastName(),
                            event.getUserId()
                    );
                    break;
                case "EMAIL_VERIFIED":
                    brevoEmailService.sendEmailVerifiedNotification(
                            event.getEmail(),
                            event.getFirstName(),
                            event.getUserId()
                    );
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consommer les événements livres
     */
    @KafkaListener(topics = "book-events", groupId = "notification-service-group")
    public void consumeBookEvent(String payload, Acknowledgment acknowledgment) {
        try {
            BookEvent event = objectMapper.readValue(payload, BookEvent.class);
            log.info("📨 Received book event: {} - {}", event.getEventType(), event.getTitle());

            switch (event.getEventType()) {
                case "BOOK_CREATED":
                    // Dans un cas réel, il faudrait récupérer l'email de l'auteur
                    String authorEmail = "author@example.com";
                    brevoEmailService.sendBookCreatedEmail(  // ⬅️ Utiliser brevoEmailService
                            authorEmail,
                            event.getTitle(),
                            event.getAuthorName()
                    );
                    break;

                case "BOOK_APPROVED":
                    log.info("📚 Book approved: {}", event.getTitle());
                    break;
                default:
                    log.warn("Unknown book event: {}", event.getEventType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing book event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consommer les événements paiement
     */
    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void consumePaymentEvent(String payload, Acknowledgment acknowledgment) {
        try {
            PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);
            log.info("📨 Received payment event: {} - Amount: {}",
                    event.getEventType(), event.getAmount());

            switch (event.getEventType()) {
                case "PAYMENT_SUCCESS":
                    brevoEmailService.sendPaymentSuccessEmail(  // ⬅️ Utiliser brevoEmailService
                            "user@example.com", // À remplacer par l'email réel
                            event.getAmount().toString(),
                            event.getCurrency(),
                            event.getTransactionId().toString()
                    );
                    break;
                case "PAYMENT_FAILED":
                    log.warn("💳 Payment failed: {}", event.getTransactionId());
                    break;
                default:
                    log.warn("Unknown payment event: {}", event.getEventType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing payment event: {}", e.getMessage(), e);
        }
    }
}