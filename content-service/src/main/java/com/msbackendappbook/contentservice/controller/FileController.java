package com.msbackendappbook.contentservice.controller;

import com.msbackendappbook.contentservice.model.dto.FileUploadResponse;
import com.msbackendappbook.contentservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/cover")
    public ResponseEntity<FileUploadResponse> uploadCover(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.uploadCover(file);
        return ResponseEntity.ok(FileUploadResponse.builder()
                .fileName(fileName)
                .fileUrl("/api/files/covers/" + fileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .success(true)
                .message("Cover uploaded successfully")
                .build());
    }

    @PostMapping("/upload/book")
    public ResponseEntity<FileUploadResponse> uploadBook(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.uploadBook(file);
        return ResponseEntity.ok(FileUploadResponse.builder()
                .fileName(fileName)
                .fileUrl("/api/files/books/" + fileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .success(true)
                .message("Book file uploaded successfully")
                .build());
    }

    // ✅ Correction : utiliser Resource et non Path
    @GetMapping("/covers/{fileName:.+}")
    public ResponseEntity<Resource> getCover(@PathVariable String fileName) {
        try {
            Resource resource = fileStorageService.loadFile(fileName, "cover");

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error loading cover: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/books/{fileName:.+}")
    public ResponseEntity<Resource> getBookFile(@PathVariable String fileName) {
        try {
            Resource resource = fileStorageService.loadFile(fileName, "book");

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error loading book file: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/covers/{fileName:.+}")
    public ResponseEntity<Void> deleteCover(@PathVariable String fileName) {
        boolean deleted = fileStorageService.deleteFile(fileName, "cover");
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/books/{fileName:.+}")
    public ResponseEntity<Void> deleteBookFile(@PathVariable String fileName) {
        boolean deleted = fileStorageService.deleteFile(fileName, "book");
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}