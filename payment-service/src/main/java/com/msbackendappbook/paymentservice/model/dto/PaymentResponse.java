package com.msbackendappbook.paymentservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID transactionId;
    private String transactionReference;
    private String status;
    private String message;
    private String paymentUrl; // URL pour rediriger l'utilisateur vers PayTech
}