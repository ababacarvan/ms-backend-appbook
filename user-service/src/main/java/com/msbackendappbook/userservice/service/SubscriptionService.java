package com.msbackendappbook.userservice.service;

import com.msbackendappbook.userservice.model.dto.SubscriptionDTO;
import com.msbackendappbook.userservice.model.dto.SubscriptionPlanDTO;
import com.msbackendappbook.userservice.model.entity.Subscription;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.model.enums.PlanType;
import com.msbackendappbook.userservice.repository.SubscriptionRepository;
import com.msbackendappbook.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${payment.service.url:http://localhost:8083}")
    private String paymentServiceUrl;

    private static final String PAYMENT_INITIATE_URL = "/api/payments/initiate";

    /**
     * Récupérer les plans d'abonnement
     */
    public List<SubscriptionPlanDTO> getPlans() {
        return Arrays.asList(
                SubscriptionPlanDTO.builder()
                        .id("monthly")
                        .name("Mensuel")
                        .price(9.99)
                        .currency("€")
                        .duration(30)
                        .features(List.of(
                                "Accès à tous les livres",
                                "Lecture hors ligne",
                                "Support prioritaire"
                        ))
                        .recommended(false)
                        .build(),
                SubscriptionPlanDTO.builder()
                        .id("yearly")
                        .name("Annuel")
                        .price(89.99)
                        .currency("€")
                        .duration(365)
                        .features(List.of(
                                "Accès à tous les livres",
                                "Lecture hors ligne",
                                "Support prioritaire",
                                "2 mois offerts"
                        ))
                        .recommended(true)
                        .build(),
                SubscriptionPlanDTO.builder()
                        .id("premium")
                        .name("Premium")
                        .price(149.99)
                        .currency("€")
                        .duration(365)
                        .features(List.of(
                                "Accès à tous les livres",
                                "Lecture hors ligne",
                                "Support VIP",
                                "Contenus exclusifs",
                                "Téléchargement illimité"
                        ))
                        .recommended(false)
                        .build()
        );
    }

    /**
     * Récupérer l'abonnement actuel de l'utilisateur
     */
    public SubscriptionDTO getCurrentSubscription(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Optional<Subscription> subscription = subscriptionRepository.findActiveSubscriptionByUser(user);

            if (subscription.isPresent()) {
                return mapToDTO(subscription.get());
            }

            // Si pas d'abonnement, créer un abonnement gratuit par défaut
            return createFreeSubscription(user);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'abonnement: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Créer un abonnement gratuit par défaut
     */
    private SubscriptionDTO createFreeSubscription(User user) {
        try {
            // Vérifier si l'utilisateur a déjà un abonnement gratuit
            Optional<Subscription> existingFree = subscriptionRepository
                    .findByUserAndActiveTrue(user)
                    .stream()
                    .filter(s -> s.getPlanType() == PlanType.FREE_TRIAL)
                    .findFirst();

            if (existingFree.isPresent()) {
                return mapToDTO(existingFree.get());
            }

            // Créer un nouvel abonnement gratuit
            Subscription freeSubscription = Subscription.builder()
                    .user(user)
                    .planType(PlanType.FREE_TRIAL)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(1))
                    .active(true)
                    .autoRenew(false)
                    .paymentMethodId("FREE_TRIAL")
                    .build();

            freeSubscription = subscriptionRepository.save(freeSubscription);
            log.info("📖 Abonnement gratuit créé pour l'utilisateur {}", user.getId());

            return mapToDTO(freeSubscription);

        } catch (Exception e) {
            log.error("Erreur lors de la création de l'abonnement gratuit: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Vérifier si un utilisateur a un abonnement actif
     */
    public boolean hasActiveSubscription(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            return subscriptionRepository.hasActiveSubscription(user);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'abonnement: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Souscrire à un abonnement
     */
    @Transactional
    public SubscriptionDTO subscribe(UUID userId, String planType, String paymentMethod, String idempotencyKey) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            PlanType type = PlanType.valueOf(planType.toUpperCase());
            double price = getPriceForPlan(type);

            // 1. Initier le paiement
            String paymentId = initiatePayment(userId, BigDecimal.valueOf(price), "XOF",
                    "Abonnement " + planType, idempotencyKey);

            if (paymentId == null) {
                throw new RuntimeException("Le paiement a échoué");
            }

            // 2. Désactiver les anciens abonnements
            List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndActiveTrue(user);
            activeSubscriptions.forEach(sub -> sub.setActive(false));
            subscriptionRepository.saveAll(activeSubscriptions);

            // 3. Créer le nouvel abonnement
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusMonths(getDurationInMonths(type));

            Subscription subscription = Subscription.builder()
                    .user(user)
                    .planType(type)
                    .startDate(startDate)
                    .endDate(endDate)
                    .active(true)
                    .autoRenew(false)
                    .paymentMethodId(paymentId)
                    .build();

            subscription = subscriptionRepository.save(subscription);
            log.info("✅ Utilisateur {} abonné au plan {} avec paiement {}", userId, planType, paymentId);

            return mapToDTO(subscription);

        } catch (Exception e) {
            log.error("Erreur lors de l'abonnement: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'abonnement: " + e.getMessage());
        }
    }

    /**
     * Initier le paiement via le service de paiement
     */
    private String initiatePayment(UUID userId, BigDecimal amount, String currency, String description, String idempotencyKey) {
        try {
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("userId", userId);
            paymentRequest.put("amount", amount);
            paymentRequest.put("currency", currency);
            paymentRequest.put("description", description);
            paymentRequest.put("idempotencyKey", idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString());
            paymentRequest.put("paymentMethod", "CARD");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentRequest, headers);

            String url = paymentServiceUrl + PAYMENT_INITIATE_URL;
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && "SUCCESS".equals(response.get("status"))) {
                return (String) response.get("transactionId");
            } else {
                log.error("Échec de l'initiation du paiement: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("Erreur d'initiation du paiement: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Annuler l'abonnement
     */
    @Transactional
    public void cancelSubscription(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Subscription subscription = subscriptionRepository.findActiveSubscriptionByUser(user)
                    .orElseThrow(() -> new RuntimeException("Aucun abonnement actif trouvé"));

            subscription.setActive(false);
            subscriptionRepository.save(subscription);
            log.info("❌ Utilisateur {} a annulé son abonnement", userId);

        } catch (Exception e) {
            log.error("Erreur lors de l'annulation de l'abonnement: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'annulation: " + e.getMessage());
        }
    }

    /**
     * Renouveler l'abonnement
     */
    @Transactional
    public SubscriptionDTO renewSubscription(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Subscription subscription = subscriptionRepository.findActiveSubscriptionByUser(user)
                    .orElseThrow(() -> new RuntimeException("Aucun abonnement actif trouvé"));

            // Prolonger la date de fin
            LocalDate newEndDate = subscription.getEndDate().plusMonths(getDurationInMonths(subscription.getPlanType()));
            subscription.setEndDate(newEndDate);
            subscription.setActive(true);

            subscription = subscriptionRepository.save(subscription);
            log.info("🔄 Utilisateur {} a renouvelé son abonnement", userId);

            return mapToDTO(subscription);

        } catch (Exception e) {
            log.error("Erreur lors du renouvellement: {}", e.getMessage());
            throw new RuntimeException("Erreur lors du renouvellement: " + e.getMessage());
        }
    }

    /**
     * Désactiver les abonnements expirés (job programmé)
     */
    @Transactional
    public void deactivateExpiredSubscriptions() {
        try {
            LocalDate today = LocalDate.now();
            List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredActiveSubscriptions(today);

            if (!expiredSubscriptions.isEmpty()) {
                expiredSubscriptions.forEach(sub -> sub.setActive(false));
                subscriptionRepository.saveAll(expiredSubscriptions);
                log.info("🗑️ {} abonnements expirés désactivés", expiredSubscriptions.size());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la désactivation des abonnements expirés: {}", e.getMessage());
        }
    }

    private int getDurationInMonths(PlanType planType) {
        return switch (planType) {
            case MONTHLY -> 1;
            case YEARLY, PREMIUM -> 12;
            case FREE_TRIAL -> 1;
        };
    }

    private double getPriceForPlan(PlanType planType) {
        return switch (planType) {
            case MONTHLY -> 9.99;
            case YEARLY -> 89.99;
            case PREMIUM -> 149.99;
            case FREE_TRIAL -> 0.0;
        };
    }

    private SubscriptionDTO mapToDTO(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionDTO.builder()
                .id(subscription.getId())
                .userId(subscription.getUser() != null ? subscription.getUser().getId() : null)
                .planType(subscription.getPlanType() != null ? subscription.getPlanType().name() : null)
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .active(subscription.isActive())
                .autoRenew(subscription.isAutoRenew())
                .paymentMethodId(subscription.getPaymentMethodId())
                .build();
    }
}