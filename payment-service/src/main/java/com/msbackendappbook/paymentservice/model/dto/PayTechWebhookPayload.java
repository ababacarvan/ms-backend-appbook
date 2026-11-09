package com.msbackendappbook.paymentservice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayTechWebhookPayload {
    private String ref_command;
    private String status;
    private String transaction_id;
    private String message;
    private String custom_field;
}