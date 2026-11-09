package com.msbackendappbook.notificationservice.controller;

import com.msbackendappbook.notificationservice.model.dto.EmailRequest;
import com.msbackendappbook.notificationservice.model.entity.Notification;
import com.msbackendappbook.notificationservice.service.BrevoEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final BrevoEmailService emailService;

    // ============================================================
    // 🔓 PUBLIC - Accessible à tous (sans authentification)
    // ============================================================

    /**
     * Vérifier l'état du service email
     * 🔓 PUBLIC
     */
    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth() {
        return ResponseEntity.ok(emailService.isEmailServiceAvailable());
    }

    /**
     * Vérifier la connexion SMTP
     * 🔓 PUBLIC
     */
    @GetMapping("/smtp/health")
    public ResponseEntity<Boolean> checkSmtpHealth() {
        return ResponseEntity.ok(emailService.checkSmtpConnection());
    }

    /**
     * Ping simple pour vérifier que le service est UP
     * 🔓 PUBLIC
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    // ============================================================
    // 🔒 AUTHENTIFIED - Utilisateur connecté
    // ============================================================

    /**
     * Récupérer les notifications d'un utilisateur
     * 🔒 AUTHENTIFIED
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        // TODO: Implémenter la récupération des notifications par userId
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Récupérer les notifications d'un utilisateur par statut
     * 🔒 AUTHENTIFIED
     */
    @GetMapping("/user/{userId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Notification>> getUserNotificationsByStatus(
            @PathVariable UUID userId,
            @PathVariable Notification.Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        // TODO: Implémenter la récupération des notifications par userId et status
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Marquer une notification comme lue
     * 🔒 AUTHENTIFIED
     */
    @PostMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        // TODO: Implémenter le marquage comme lu
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // 🔒 ADMIN - Réservé aux administrateurs
    // ============================================================

    /**
     * Envoyer un email simple
     * 🔒 ADMIN
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notification> sendEmail(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(emailService.sendEmail(request));
    }

    /**
     * Envoyer un email avec template
     * 🔒 ADMIN
     */
    @PostMapping("/send/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notification> sendEmailWithTemplate(
            @RequestParam String to,
            @RequestParam String templateName,
            @RequestBody(required = false) Map<String, Object> params) {
        return ResponseEntity.ok(emailService.sendEmailWithTemplate(to, templateName, params, null));
    }

    /**
     * Envoyer un email avec template et userId
     * 🔒 ADMIN
     */
    @PostMapping("/send/template/user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notification> sendEmailWithTemplateAndUser(
            @RequestParam String to,
            @RequestParam String templateName,
            @RequestParam UUID userId,
            @RequestBody(required = false) Map<String, Object> params) {
        return ResponseEntity.ok(emailService.sendEmailWithTemplate(to, templateName, params, userId));
    }

    /**
     * Envoyer un email de bienvenue (test)
     * 🔒 ADMIN
     */
    @PostMapping("/test/welcome")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notification> testWelcomeEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "Test") String firstName,
            @RequestParam(defaultValue = "User") String lastName) {
        return ResponseEntity.ok(
                emailService.sendEmailWithTemplate(
                        email,
                        "WELCOME",
                        Map.of(
                                "firstName", firstName,
                                "lastName", lastName,
                                "email", email,
                                "year", String.valueOf(java.time.LocalDateTime.now().getYear())
                        ),
                        UUID.randomUUID()
                )
        );
    }

    /**
     * Récupérer toutes les notifications (admin)
     * 🔒 ADMIN
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Notification>> getAllNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        // TODO: Implémenter la récupération de toutes les notifications
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Récupérer les statistiques des notifications
     * 🔒 ADMIN
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        // TODO: Implémenter les statistiques
        return ResponseEntity.ok(Map.of(
                "total", 0,
                "sent", 0,
                "pending", 0,
                "failed", 0
        ));
    }

    /**
     * Supprimer une notification
     * 🔒 ADMIN
     */
    @DeleteMapping("/admin/{notificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        // TODO: Implémenter la suppression
        return ResponseEntity.noContent().build();
    }
}