package com.msbackendappbook.gatewayservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback/user")
    public Mono<Map<String, Object>> userFallback() {
        return Mono.just(errorResponse("USER_SERVICE_UNAVAILABLE", "User service unavailable"));
    }

    @GetMapping("/fallback/content")
    public Mono<Map<String, Object>> contentFallback() {
        return Mono.just(errorResponse("CONTENT_SERVICE_UNAVAILABLE", "Content service unavailable"));
    }

    @GetMapping("/fallback/payment")
    public Mono<Map<String, Object>> paymentFallback() {
        return Mono.just(errorResponse("PAYMENT_SERVICE_UNAVAILABLE", "Payment service unavailable"));
    }

    @GetMapping("/fallback/notification")
    public Mono<Map<String, Object>> notificationFallback() {
        return Mono.just(errorResponse("NOTIFICATION_SERVICE_UNAVAILABLE", "Notification service unavailable"));
    }

    @GetMapping("/fallback/analytics")
    public Mono<Map<String, Object>> analyticsFallback() {
        return Mono.just(errorResponse("ANALYTICS_SERVICE_UNAVAILABLE", "Analytics service unavailable"));
    }

    @GetMapping("/fallback/default")
    public Mono<Map<String, Object>> defaultFallback() {
        return Mono.just(errorResponse("SERVICE_UNAVAILABLE", "Service temporarily unavailable"));
    }

    private Map<String, Object> errorResponse(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}