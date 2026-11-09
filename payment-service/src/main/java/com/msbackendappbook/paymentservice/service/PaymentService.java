package com.msbackendappbook.paymentservice.service;

import com.msbackendappbook.paymentservice.exception.PaymentException;
import com.msbackendappbook.paymentservice.model.dto.PayTechWebhookPayload;
import com.msbackendappbook.paymentservice.model.dto.PaymentRequest;
import com.msbackendappbook.paymentservice.model.dto.PaymentResponse;
import com.msbackendappbook.paymentservice.model.entity.PaymentTransaction;
import com.msbackendappbook.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final PayTechClient payTechClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // 1. Vérification de l'idempotence
        if (request.getIdempotencyKey() != null) {
            var existingTransaction = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                log.warn("Duplicate payment request detected for key: {}", request.getIdempotencyKey());
                return mapToResponse(existingTransaction.get());
            }
        }

        // 2. Générer une référence unique et créer l'entité
        String transactionRef = generateTransactionReference();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionReference(transactionRef)
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING")
                .idempotencyKey(request.getIdempotencyKey())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        // 3. Appeler l'API PayTech
        try {
            Map<String, Object> paytechResponse = payTechClient.initiatePayment(
                    transactionRef,
                    request.getAmount(),
                    request.getDescription() != null ? request.getDescription() : "Payment for AppBook",
                    request.getUserId().toString()
            );

            // Mettre à jour la transaction avec la réponse du fournisseur
            transaction.setProviderResponse(paytechResponse.toString());
            transaction.setStatus(paytechResponse.get("status") != null ? paytechResponse.get("status").toString() : "PENDING");
            transactionRepository.save(transaction);

            // Si PayTech fournit une URL de redirection
            String paymentUrl = (String) paytechResponse.get("payment_url");

            // 4. Envoyer un événement Kafka (asynchrone)
            sendKafkaEvent(transaction, "PAYMENT_INITIATED");

            return PaymentResponse.builder()
                    .transactionId(transaction.getId())
                    .transactionReference(transactionRef)
                    .status(transaction.getStatus())
                    .message("Payment initiated successfully")
                    .paymentUrl(paymentUrl)
                    .build();

        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setProviderResponse("Error: " + e.getMessage());
            transactionRepository.save(transaction);
            log.error("Payment initiation failed for transaction: {}", transactionRef, e);
            throw new PaymentException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Transactional
    public void processWebhook(PayTechWebhookPayload payload) {
        log.info("Received PayTech webhook: {}", payload);

        // Vérifier que la transaction existe
        PaymentTransaction transaction = transactionRepository
                .findByTransactionReference(payload.getRef_command())
                .orElseThrow(() -> new PaymentException("Transaction not found: " + payload.getRef_command()));

        // Mettre à jour le statut en fonction de la réponse de PayTech
        String newStatus = "PENDING";
        if ("success".equalsIgnoreCase(payload.getStatus())) {
            newStatus = "SUCCESS";
        } else if ("cancel".equalsIgnoreCase(payload.getStatus()) || "failed".equalsIgnoreCase(payload.getStatus())) {
            newStatus = "FAILED";
        }

        transaction.setStatus(newStatus);
        transaction.setProviderResponse(payload.toString());
        transactionRepository.save(transaction);

        // Envoyer un événement Kafka pour informer les autres services
        sendKafkaEvent(transaction, "PAYMENT_STATUS_UPDATED");
    }

    // --- Méthodes utilitaires privées ---

    private String generateTransactionReference() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private void sendKafkaEvent(PaymentTransaction transaction, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("transactionId", transaction.getId().toString());
            event.put("transactionReference", transaction.getTransactionReference());
            event.put("userId", transaction.getUserId().toString());
            event.put("amount", transaction.getAmount());
            event.put("currency", transaction.getCurrency());
            event.put("status", transaction.getStatus());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send("payment-events", transaction.getUserId().toString(), event);
            log.info("Kafka event sent: {}", event);
        } catch (Exception e) {
            log.error("Failed to send Kafka event", e);
        }
    }

    private PaymentResponse mapToResponse(PaymentTransaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .status(transaction.getStatus())
                .message("Payment already processed")
                .build();
    }
}