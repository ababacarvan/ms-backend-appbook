package com.msbackendappbook.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "paytech")
@Data
public class PayTechConfig {
    private String apiUrl;
    private String apiKey;
    private String secretKey;
    private String merchantId;
    private String successUrl;
    private String cancelUrl;
    private String ipnUrl;
}