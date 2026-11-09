package com.msbackendappbook.contentservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageConfig fileStorageConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Servir les fichiers uploadés via une URL
        registry.addResourceHandler("/api/files/covers/**")
                .addResourceLocations("file:" + fileStorageConfig.getCoverLocation() + "/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/api/files/books/**")
                .addResourceLocations("file:" + fileStorageConfig.getBookLocation() + "/")
                .setCachePeriod(3600);
    }
}