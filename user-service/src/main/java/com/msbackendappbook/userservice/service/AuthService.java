package com.msbackendappbook.userservice.service;

import com.msbackendappbook.userservice.exceptions.AuthenticationException;
import com.msbackendappbook.userservice.mapper.UserMapper;
import com.msbackendappbook.userservice.model.dto.AuthenticationResponse;
import com.msbackendappbook.userservice.model.dto.LoginRequest;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;

    public AuthenticationResponse login(LoginRequest loginRequest){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(()->new AuthenticationException("User not found"));

            //Update last login
            userService.updateLoginTimestamp(user.getId());

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userMapper.mapToDTO(user))
                    .message("Login Successful")
                    .build();

        } catch (Exception e) {
            throw new AuthenticationException("Invalid email or password") {
            };
        }
    }

    public AuthenticationResponse refreshToken(String refreshToken){
        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(()-> new AuthenticationException("User not found"));
            if(!jwtService.isTokenValid(refreshToken, user)){
                throw new RuntimeException("Invalid token");
            }
            String newAccessToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            return AuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .user(userMapper.mapToDTO(user))
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Invalid refresh token: " + e.getMessage());
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        // Could add blacklisting logic here
        // For JWT stateless approach, client-side token removal is enough
    }
}
