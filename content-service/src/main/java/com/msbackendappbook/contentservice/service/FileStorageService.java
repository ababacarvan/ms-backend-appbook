package com.msbackendappbook.contentservice.service;

import com.msbackendappbook.contentservice.config.FileStorageConfig;
import com.msbackendappbook.contentservice.exception.FileStorageException;
import com.msbackendappbook.contentservice.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageConfig fileStorageConfig;
    private final FileValidator fileValidator;

    public String uploadFile(MultipartFile file, String type) {
        try {
            fileValidator.validate(file, type);

            String filename = generateFileName(file.getOriginalFilename());
            String subPath = getSubPath(type);

            if (fileStorageConfig.isUseCloudStorage()) {
                return uploadToCloud(file, filename, subPath);
            } else {
                return uploadToLocal(file, filename, subPath);
            }

        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload {}: {}", type, e.getMessage(), e);
            throw FileStorageException.uploadFailed(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
            );
        }
    }

    public String uploadCover(MultipartFile file) {
        return uploadFile(file, "cover");
    }

    public String uploadBook(MultipartFile file) {
        return uploadFile(file, "book");
    }

    /**
     * Upload local avec création automatique des dossiers
     */
    private String uploadToLocal(MultipartFile file, String filename, String subPath) throws IOException {
        // ✅ 1. Créer le dossier s'il n'existe pas
        Path uploadPath = Paths.get(subPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("📁 Created directory: {}", uploadPath.toAbsolutePath());
        }

        // ✅ 2. Sauvegarder le fichier
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("📁 File saved: {}", filePath.toAbsolutePath());
        return filename;
    }

    private String uploadToCloud(MultipartFile file, String filename, String subPath) {
        log.info("☁️ Uploading to cloud: {}", filename);
        return filename;
    }

    private String getSubPath(String type) {
        return switch (type) {
            case "cover" -> fileStorageConfig.getCoverLocation();
            case "book" -> fileStorageConfig.getBookLocation();
            default -> fileStorageConfig.getLocation();
        };
    }

    private String generateFileName(String originalFilename) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return timestamp + "_" + uuid + extension;
    }

    public Resource loadFile(String filename, String type) {
        try {
            String subPath = getSubPath(type);
            Path filePath = Paths.get(subPath).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.warn("File not found: {}", filePath.toAbsolutePath());
                throw FileStorageException.fileNotFound(filename);
            }
        } catch (Exception e) {
            throw FileStorageException.fileNotFound(filename);
        }
    }

    public boolean deleteFile(String filename, String type) {
        try {
            String subPath = getSubPath(type);
            Path filePath = Paths.get(subPath).resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteFileByUrl(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return true;
            }
            String fileName = extractFileNameFromUrl(fileUrl);
            String type = extractTypeFromUrl(fileUrl);
            return deleteFile(fileName, type);
        } catch (Exception e) {
            log.error("Failed to delete file by URL: {}", e.getMessage());
            return false;
        }
    }

    private String extractFileNameFromUrl(String fileUrl) {
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }

    private String extractTypeFromUrl(String fileUrl) {
        if (fileUrl.contains("/covers/")) {
            return "cover";
        } else if (fileUrl.contains("/books/")) {
            return "book";
        }
        return "file";
    }
}