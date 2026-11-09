package com.msbackendappbook.notificationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID eventId;
    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String status; // SUCCESS, FAILED, REFUNDED
    private String eventType; // PAYMENT_SUCCESS, PAYMENT_FAILED
    private LocalDateTime timestamp;
}