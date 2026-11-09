package com.msbackendappbook.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanDTO {
    private String id;
    private String name;
    private double price;
    private String currency;
    private int duration;

    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Builder.Default
    private boolean recommended = false;
}