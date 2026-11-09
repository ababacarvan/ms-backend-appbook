package com.msbackendappbook.contentservice.util;

import com.msbackendappbook.contentservice.config.FileStorageConfig;
import com.msbackendappbook.contentservice.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileValidator {

    private final FileStorageConfig fileStorageConfig;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> ALLOWED_BOOK_TYPES = Set.of(
            "application/pdf",
            "application/epub+zip",
            "application/vnd.mobi",
            "audio/mpeg",
            "audio/mp4"
    );

    public void validate(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw FileStorageException.fileEmpty();
        }

        String originalFilename = file.getOriginalFilename();
        log.info("📁 Validating file: {}, type: {}, size: {}", originalFilename, type, file.getSize());

        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw FileStorageException.invalidFileName("Empty filename");
        }

        // ✅ Vérifier la taille
        long maxSize = type.equals("cover") ?
                fileStorageConfig.getMaxCoverSize() :
                fileStorageConfig.getMaxFileSize();

        if (file.getSize() > maxSize) {
            throw FileStorageException.fileTooLarge(maxSize);
        }

        String contentType = file.getContentType();

        if (type.equals("cover")) {
            // ✅ Vérifier le type MIME
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                log.error("❌ Invalid content type: {}", contentType);
                throw FileStorageException.invalidFileType(
                        contentType != null ? contentType : "unknown"
                );
            }

            // ✅ Vérifier l'extension (insensible à la casse)
            String ext = getFileExtension(originalFilename);
            log.info("📁 File extension: {}", ext);

            if (ext == null || ext.isEmpty()) {
                log.error("❌ No file extension found for: {}", originalFilename);
                throw new FileStorageException(
                        "Invalid file name: " + originalFilename + ". Missing file extension",
                        "INVALID_FILE_NAME",
                        org.springframework.http.HttpStatus.BAD_REQUEST
                );
            }

            // ✅ Vérifier l'extension (version insensible à la casse)
            String extLower = ext.toLowerCase();
            if (!extLower.equals(".jpg") && !extLower.equals(".jpeg") &&
                    !extLower.equals(".png") && !extLower.equals(".webp") &&
                    !extLower.equals(".gif")) {
                log.error("❌ Invalid extension: {}", ext);
                throw new FileStorageException(
                        "Invalid image extension: " + ext + ". Allowed: .jpg, .jpeg, .png, .webp, .gif",
                        "INVALID_FILE_EXTENSION",
                        org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
                );
            }

        } else if (type.equals("book")) {
            if (contentType == null || !ALLOWED_BOOK_TYPES.contains(contentType)) {
                throw FileStorageException.invalidFileType(
                        contentType != null ? contentType : "unknown"
                );
            }

            String ext = getFileExtension(originalFilename);
            String extLower = ext != null ? ext.toLowerCase() : "";
            if (!extLower.equals(".pdf") && !extLower.equals(".epub") &&
                    !extLower.equals(".mobi") && !extLower.equals(".mp3") &&
                    !extLower.equals(".m4a")) {
                throw new FileStorageException(
                        "Invalid book extension: " + ext + ". Allowed: .pdf, .epub, .mobi, .mp3, .m4a",
                        "INVALID_FILE_EXTENSION",
                        org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
                );
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return null;
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDotIndex);
    }
}