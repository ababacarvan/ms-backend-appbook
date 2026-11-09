package com.msbackendappbook.paymentservice.service;

import com.msbackendappbook.paymentservice.config.PayTechConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayTechClient {

    private final PayTechConfig payTechConfig;
    private final RestTemplate restTemplate;

    /**
     * Initialise une demande de paiement auprès de PayTech.
     * @param transactionReference La référence unique de la transaction.
     * @param amount Le montant en XOF.
     * @param description La description du paiement.
     * @param customField Un champ personnalisé (peut contenir l'ID utilisateur).
     * @return La réponse de l'API PayTech.
     */
    public Map<String, Object> initiatePayment(String transactionReference, BigDecimal amount, String description, String customField) {
        String url = payTechConfig.getApiUrl() + "/payment/request-payment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("API-KEY", payTechConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("item_name", description);
        requestBody.put("item_price", amount.intValue());
        requestBody.put("currency", "XOF");
        requestBody.put("ref_command", transactionReference);
        requestBody.put("command_name", "Payment for AppBook subscription");
        requestBody.put("ipn_url", payTechConfig.getIpnUrl());
        requestBody.put("success_url", payTechConfig.getSuccessUrl());
        requestBody.put("cancel_url", payTechConfig.getCancelUrl());
        requestBody.put("custom_field", customField); // On peut y mettre l'userId ou tout autre info

        log.info("Sending payment request to PayTech: {}", requestBody);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("PayTech response: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("Error from PayTech: {}", response.getStatusCode());
                throw new RuntimeException("Failed to initiate payment with PayTech");
            }
        } catch (Exception e) {
            log.error("Exception while calling PayTech API", e);
            throw new RuntimeException("Error calling PayTech API: " + e.getMessage());
        }
    }
}