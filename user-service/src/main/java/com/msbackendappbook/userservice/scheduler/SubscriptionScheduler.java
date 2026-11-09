package com.msbackendappbook.userservice.scheduler;

import com.msbackendappbook.userservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Désactiver les abonnements expirés tous les jours à minuit
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void deactivateExpiredSubscriptions() {
        log.info("🔄 Vérification des abonnements expirés...");
        subscriptionService.deactivateExpiredSubscriptions();
    }
}