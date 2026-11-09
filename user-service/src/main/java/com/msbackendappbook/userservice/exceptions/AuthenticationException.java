// AuthenticationException.java
package com.msbackendappbook.userservice.exceptions;

import lombok.Getter;

@Getter
public class AuthenticationException extends RuntimeException {
    private final String path;

    public AuthenticationException(String message) {
        super(message);
        this.path = "";
    }

    public AuthenticationException(String message, String path) {
        super(message);
        this.path = path;
    }
}