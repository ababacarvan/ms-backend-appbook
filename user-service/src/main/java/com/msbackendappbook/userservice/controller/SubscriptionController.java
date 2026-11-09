package com.msbackendappbook.userservice.controller;

import com.msbackendappbook.userservice.model.dto.SubscriptionDTO;
import com.msbackendappbook.userservice.model.dto.SubscriptionPlanDTO;
import com.msbackendappbook.userservice.model.dto.SubscriptionRequest;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.repository.UserRepository;
import com.msbackendappbook.userservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    /**
     * Récupérer les plans d'abonnement
     */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDTO>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getPlans());
    }

    /**
     * Récupérer l'abonnement actuel de l'utilisateur
     */
    @GetMapping("/current")
    public ResponseEntity<SubscriptionDTO> getCurrentSubscription() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SubscriptionDTO subscription = subscriptionService.getCurrentSubscription(userId);
        if (subscription == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(subscription);
    }

    /**
     * Vérifier si l'utilisateur a un abonnement actif
     */
    @GetMapping("/status")
    public ResponseEntity<Boolean> hasActiveSubscription() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(subscriptionService.hasActiveSubscription(userId));
    }

    /**
     * Souscrire à un abonnement
     */
    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionDTO> subscribe(@RequestBody SubscriptionRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(
                        userId,
                        request.getPlanType(),
                        request.getPaymentMethod(),
                        request.getIdempotencyKey()
                ));
    }

    /**
     * Annuler l'abonnement
     */
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        subscriptionService.cancelSubscription(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Renouveler l'abonnement
     */
    @PostMapping("/renew")
    public ResponseEntity<SubscriptionDTO> renewSubscription() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(subscriptionService.renewSubscription(userId));
    }

    /**
     * Récupérer l'ID de l'utilisateur connecté
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String email = auth.getName();
        if (email == null || email.isEmpty()) {
            return null;
        }

        // Récupérer l'utilisateur par email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }

        return user.getId();
    }
}