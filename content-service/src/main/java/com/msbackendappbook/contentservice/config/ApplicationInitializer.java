package com.msbackendappbook.contentservice.config;

import com.msbackendappbook.contentservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitializer {

    private final FileStorageConfig fileStorageConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDirectories() {
        log.info("📁 Initialisation des dossiers de stockage...");

        String[] directories = {
                fileStorageConfig.getLocation(),
                fileStorageConfig.getCoverLocation(),
                fileStorageConfig.getBookLocation(),
                fileStorageConfig.getTempLocation()
        };

        for (String dir : directories) {
            try {
                Path path = Paths.get(dir);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    log.info("✅ Dossier créé: {}", path.toAbsolutePath());
                } else {
                    log.info("✅ Dossier existe déjà: {}", path.toAbsolutePath());
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la création du dossier {}: {}", dir, e.getMessage());
            }
        }

        log.info("✅ Initialisation des dossiers terminée !");
    }
}