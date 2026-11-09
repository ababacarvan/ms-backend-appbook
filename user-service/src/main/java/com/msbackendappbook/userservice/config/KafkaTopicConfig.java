package com.msbackendappbook.userservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("!docker")
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;
    private static final String RETENTION_7_DAYS = "604800000";
    private static final String RETENTION_30_DAYS = "2592000000";



    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.CLIENT_ID_CONFIG, "user-service-admin");
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
        kafkaAdmin.setFatalIfBrokerNotAvailable(false);
        kafkaAdmin.setAutoCreate(false);
        return kafkaAdmin;
    }


    // ===== TOPICS PRINCIPAUX =====

    @Bean
    public NewTopic userEventsTopic() {
        return createTopic("user-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    @Bean
    public NewTopic authEventsTopic() {
        return createTopic("auth-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    @Bean
    public NewTopic bookEventsTopic() {
        return createTopic("book-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return createTopic("payment-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    @Bean
    public NewTopic subscriptionEventsTopic() {
        return createTopic("subscription-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return createTopic("notification-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    @Bean
    public NewTopic analyticsEventsTopic() {
        return createTopic("analytics-events", PARTITIONS, REPLICAS, RETENTION_7_DAYS);
    }

    // ===== DEAD LETTER QUEUES =====

    @Bean
    public NewTopic userEventsDLQTopic() {
        return createTopic("user-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic authEventsDLQTopic() {
        return createTopic("auth-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic bookEventsDLQTopic() {
        return createTopic("book-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic paymentEventsDLQTopic() {
        return createTopic("payment-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic subscriptionEventsDLQTopic() {
        return createTopic("subscription-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic notificationEventsDLQTopic() {
        return createTopic("notification-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic analyticsEventsDLQTopic() {
        return createTopic("analytics-events-dlq", 1, REPLICAS, RETENTION_30_DAYS);
    }

    @Bean
    public NewTopic defaultEventsTopic() {
        return TopicBuilder.name("default-events")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", RETENTION_7_DAYS)
                .config("cleanup.policy", "delete")
                .build();
    }

    // ===== MÉTHODE UTILITAIRE =====

    private NewTopic createTopic(String name, int partitions, int replicas, String retentionMs) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .config("retention.ms", retentionMs)
                .config("cleanup.policy", "delete")
                .build();
    }
}