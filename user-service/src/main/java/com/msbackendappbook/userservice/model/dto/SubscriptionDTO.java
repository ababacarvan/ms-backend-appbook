package com.msbackendappbook.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {
    private UUID id;
    private UUID userId;
    private String planType;
    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private boolean active = false;

    @Builder.Default
    private boolean autoRenew = false;

    private String paymentMethodId;
}