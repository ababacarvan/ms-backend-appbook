package com.msbackendappbook.contentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "trial_used")
    private boolean trialUsed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SubscriptionPlan {
        FREE("Gratuit", 0.0, 2),      // 2% du livre
        BASIC("Basic", 5.0, 30),      // 30% du livre
        PREMIUM("Premium", 15.0, 100); // 100% du livre

        private final String label;
        private final double price;
        private final int accessPercentage;

        SubscriptionPlan(String label, double price, int accessPercentage) {
            this.label = label;
            this.price = price;
            this.accessPercentage = accessPercentage;
        }

        public String getLabel() { return label; }
        public double getPrice() { return price; }
        public int getAccessPercentage() { return accessPercentage; }
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return active && !isExpired();
    }

    public int getMaxAccessPercentage() {
        return plan != null ? plan.getAccessPercentage() : 2;
    }
}