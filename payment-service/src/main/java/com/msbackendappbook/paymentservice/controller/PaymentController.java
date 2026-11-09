package com.msbackendappbook.paymentservice.controller;

import com.msbackendappbook.paymentservice.model.dto.PayTechWebhookPayload;
import com.msbackendappbook.paymentservice.model.dto.PaymentRequest;
import com.msbackendappbook.paymentservice.model.dto.PaymentResponse;
import com.msbackendappbook.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment initiation request: {}", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(request));
    }

    @PostMapping("/webhook/paytech")
    public ResponseEntity<Void> handlePayTechWebhook(@RequestBody PayTechWebhookPayload payload) {
        log.info("Received webhook from PayTech: {}", payload);
        try {
            paymentService.processWebhook(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}