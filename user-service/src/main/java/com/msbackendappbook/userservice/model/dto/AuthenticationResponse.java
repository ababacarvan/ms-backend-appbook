package com.msbackendappbook.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
    private String message;
}
