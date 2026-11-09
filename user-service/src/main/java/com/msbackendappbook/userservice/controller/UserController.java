package com.msbackendappbook.userservice.controller;

import com.msbackendappbook.userservice.exceptions.ResourceNotFoundException;
import com.msbackendappbook.userservice.mapper.UserMapper;
import com.msbackendappbook.userservice.model.dto.*;
import com.msbackendappbook.userservice.model.entity.ReadingProgress;
import com.msbackendappbook.userservice.model.enums.Status;
import com.msbackendappbook.userservice.service.AuthService;
import com.msbackendappbook.userservice.service.ReadingService;
import com.msbackendappbook.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    private final ReadingService readingService;
    private final UserMapper userMapper;

    // ===== AUTH ENDPOINTS =====

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO userDTO = userService.register(request);
        AuthenticationResponse response = AuthenticationResponse.builder()
                .user(userDTO)
                .message("Register Successful!!! Please verify your email")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody String refreshToken) {
        AuthenticationResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }

    // ===== USER PROFILE ENDPOINTS =====

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ===== VERIFICATION ENDPOINT =====

    @GetMapping("/verify/{userId}")
    public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable UUID userId) {
        try {
            userService.verifyEmail(userId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "✅ Votre compte a été activé avec succès !");
            response.put("userId", userId.toString());

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "❌ Utilisateur non trouvé");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "❌ Erreur lors de l'activation : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===== READING PROGRESS ENDPOINTS =====

    @PostMapping("/{userId}/reading/{bookId}/start")
    public ResponseEntity<Void> startReading(
            @PathVariable UUID userId,
            @PathVariable UUID bookId,
            @RequestParam int totalPages) {
        userService.startReading(userId, bookId, totalPages);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/reading/{bookId}/progress")
    public ResponseEntity<Void> updateReadingProgress(
            @PathVariable UUID userId,
            @PathVariable UUID bookId,
            @RequestParam int currentPage) {
        userService.updateReadingProgress(userId, bookId, currentPage);
        return ResponseEntity.ok().build();
    }

    /**
     * Vérifier l'accès à un livre
     * GET /api/reading/{bookId}/access
     */
    @GetMapping("/reading/{bookId}/access")
    public ResponseEntity<BookAccess> checkAccess(
            @PathVariable UUID bookId,
            @RequestParam int totalPages) {
        UUID userId = getCurrentUserId();
        BookAccess access = readingService.checkBookAccess(userId, bookId, totalPages);
        return ResponseEntity.ok(access);
    }

    /**
     * Démarrer la lecture
     * POST /api/reading/{bookId}/start
     */
    @PostMapping("/reading/{bookId}/start")
    public ResponseEntity<ReadingProgressDTO> startReading(
            @PathVariable UUID bookId,
            @RequestParam int totalPages) {
        UUID userId = getCurrentUserId();
        ReadingProgress progress = readingService.startReading(userId, bookId, totalPages);
        return ResponseEntity.ok(mapToDTO(progress));
    }

    /**
     * Mettre à jour la progression
     * PUT /api/reading/{bookId}/progress
     */
    @PutMapping("/reading/{bookId}/progress")
    public ResponseEntity<ReadingProgressDTO> updateProgress(
            @PathVariable UUID bookId,
            @RequestParam int currentPage) {
        UUID userId = getCurrentUserId();
        ReadingProgress progress = readingService.updateProgress(userId, bookId, currentPage);
        return ResponseEntity.ok(mapToDTO(progress));
    }

    /**
     * Obtenir la progression
     * GET /api/reading/{bookId}/progress
     */
    @GetMapping("/reading/{bookId}/progress")
    public ResponseEntity<ReadingProgressDTO> getProgress(@PathVariable UUID bookId) {
        UUID userId = getCurrentUserId();
        ReadingProgress progress = readingService.getProgress(userId, bookId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDTO(progress));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        // TODO: Récupérer l'userId à partir de l'email
        // Pour l'instant, on simule
        return UUID.randomUUID();
    }

    private ReadingProgressDTO mapToDTO(ReadingProgress progress) {
        return ReadingProgressDTO.builder()
                .id(progress.getId())
                .userId(progress.getUser().getId())
                .bookId(progress.getBookId())
                .currentPage(progress.getCurrentPage())
                .totalPages(progress.getTotalPages())
                .percentageRead(progress.getPercentageRead())
                .status(progress.getStatus())
                .lastReadAt(progress.getLastReadAt())
                .hasSubscription(progress.isHasSubscription())
                .maxReadablePages(progress.getMaxReadablePages())
                .isCompleted(progress.getStatus() == Status.COMPLETED)
                .build();
    }
}