// ResourceNotFoundException.java
package com.msbackendappbook.userservice.exceptions;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String path;

    public ResourceNotFoundException(String message) {
        super(message);
        this.path = "";
    }

    public ResourceNotFoundException(String message, String path) {
        super(message);
        this.path = path;
    }
}