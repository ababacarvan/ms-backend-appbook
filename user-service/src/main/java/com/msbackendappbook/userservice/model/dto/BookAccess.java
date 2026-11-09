package com.msbackendappbook.userservice.model.dto;

import com.msbackendappbook.userservice.model.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAccess {
    @Builder.Default
    private boolean canRead = false;

    private String reason;

    @Builder.Default
    private int maxPages = 0;

    @Builder.Default
    private boolean isSubscriber = false;

    private PlanType subscriptionType;

    @Builder.Default
    private boolean hasUsedPreview = false;

    @Builder.Default
    private int previewPages = 0;
}