package com.msbackendappbook.contentservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FileStorageException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public FileStorageException(String message) {
        super(message);
        this.errorCode = "FILE_STORAGE_ERROR";
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FILE_STORAGE_ERROR";
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public FileStorageException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public FileStorageException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public FileStorageException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    // ===== ERREURS SPÉCIFIQUES =====

    public static FileStorageException fileNotFound(String filename) {
        return new FileStorageException(
                "File not found: " + filename,
                "FILE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static FileStorageException fileEmpty() {
        return new FileStorageException(
                "File is empty",
                "FILE_EMPTY",
                HttpStatus.BAD_REQUEST
        );
    }

    public static FileStorageException invalidFileType(String contentType) {
        return new FileStorageException(
                "Invalid file type: " + contentType,
                "INVALID_FILE_TYPE",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE
        );
    }

    public static FileStorageException fileTooLarge(long maxSize) {
        return new FileStorageException(
                "File size exceeds maximum allowed: " + (maxSize / 1024 / 1024) + " MB",
                "FILE_TOO_LARGE",
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    public static FileStorageException uploadFailed(String filename) {
        return new FileStorageException(
                "Failed to upload file: " + filename,
                "UPLOAD_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    public static FileStorageException deleteFailed(String filename) {
        return new FileStorageException(
                "Failed to delete file: " + filename,
                "DELETE_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    public static FileStorageException cloudStorageError(String message) {
        return new FileStorageException(
                "Cloud storage error: " + message,
                "CLOUD_STORAGE_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    public static FileStorageException invalidFileName(String filename) {
        return new FileStorageException(
                "Invalid file name: " + filename,
                "INVALID_FILE_NAME",
                HttpStatus.BAD_REQUEST
        );
    }

    public static FileStorageException directoryCreationFailed(String path) {
        return new FileStorageException(
                "Failed to create directory: " + path,
                "DIRECTORY_CREATION_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}