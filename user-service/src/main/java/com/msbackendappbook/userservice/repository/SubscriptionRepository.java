package com.msbackendappbook.userservice.repository;

import com.msbackendappbook.userservice.model.entity.Subscription;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.model.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Récupérer tous les abonnements actifs d'un utilisateur
     */
    List<Subscription> findByUserAndActiveTrue(User user);

    /**
     * Récupérer un abonnement actif d'un utilisateur
     */
    Optional<Subscription> findOneByUserAndActiveTrue(User user);

    /**
     * Vérifier si un utilisateur a un abonnement actif
     */
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.user = :user AND s.active = true")
    boolean hasActiveSubscription(@Param("user") User user);

    /**
     * Récupérer l'abonnement actif d'un utilisateur (avec @Query explicite)
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.active = true")
    Optional<Subscription> findActiveSubscriptionByUser(@Param("user") User user);

    /**
     * Récupérer tous les abonnements actifs expirés
     */
    @Query("SELECT s FROM Subscription s WHERE s.endDate < :date AND s.active = true")
    List<Subscription> findExpiredActiveSubscriptions(@Param("date") LocalDate date);

    /**
     * Récupérer les abonnements expirés d'un utilisateur spécifique
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.endDate < :date AND s.active = true")
    List<Subscription> findExpiredActiveSubscriptionsByUser(@Param("user") User user,
                                                            @Param("date") LocalDate date);

    /**
     * Désactiver les abonnements expirés
     */
    @Modifying
    @Transactional
    @Query("UPDATE Subscription s SET s.active = false WHERE s.endDate < :date AND s.active = true")
    void deactivateExpiredSubscriptions(@Param("date") LocalDate date);

    /**
     * Récupérer un abonnement par son ID et utilisateur
     */
    Optional<Subscription> findByIdAndUser(UUID id, User user);

    /**
     * Récupérer tous les abonnements par type de plan
     */
    List<Subscription> findByPlanTypeAndActiveTrue(PlanType planType);

    /**
     * Récupérer les abonnements qui expirent bientôt (dans les 7 jours)
     */
    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :start AND :end AND s.active = true")
    List<Subscription> findExpiringSoon(@Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    /**
     * Compter les abonnements actifs par type
     */
    @Query("SELECT s.planType, COUNT(s) FROM Subscription s WHERE s.active = true GROUP BY s.planType")
    List<Object[]> countActiveSubscriptionsByPlanType();

    /**
     * Compter le nombre total d'abonnements actifs
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.active = true")
    long countActiveSubscriptions();
}