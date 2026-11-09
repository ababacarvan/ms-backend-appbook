package com.msbackendappbook.contentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageConfig {
    private String location = "../uploads";
    private String coverLocation = "../uploads/covers";
    private String bookLocation = "../uploads/books";
    private String tempLocation = "../uploads/temp";
    private long maxFileSize = 52428800; // 50MB
    private long maxCoverSize = 10485760; // 10MB
    private boolean useCloudStorage = false;
    private CloudConfig cloud = new CloudConfig();

    @Data
    public static class CloudConfig {
        private String provider = "aws";
        private String region = "eu-west-3";
        private String bucketName = "appbook";
        private String accessKey = "";
        private String secretKey = "";
        private String endpoint = "";
    }
}