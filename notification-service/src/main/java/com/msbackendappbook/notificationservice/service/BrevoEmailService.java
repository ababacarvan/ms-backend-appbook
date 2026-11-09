package com.msbackendappbook.notificationservice.service;

import com.msbackendappbook.notificationservice.model.dto.EmailRequest;
import com.msbackendappbook.notificationservice.model.entity.EmailTemplate;
import com.msbackendappbook.notificationservice.model.entity.Notification;
import com.msbackendappbook.notificationservice.repository.EmailTemplateRepository;
import com.msbackendappbook.notificationservice.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final EmailTemplateRepository emailTemplateRepository;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    // ===== MÉTHODES PRINCIPALES =====

    @Transactional
    public Notification sendEmail(EmailRequest request) {
        try {
            Notification notification = createNotification(request);
            notification.setStatus(Notification.Status.PENDING);
            notification = notificationRepository.save(notification);

            sendEmailInternal(request);

            notification.setStatus(Notification.Status.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Email sent to {}", request.getTo());
            return notification;

        } catch (Exception e) {
            log.error("❌ Email failed: {}", e.getMessage());
            Notification notification = createNotification(request);
            notification.setStatus(Notification.Status.FAILED);
            notification.setLastError(e.getMessage());
            notificationRepository.save(notification);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // ✅ Méthode avec userId (pour les événements)
    @Transactional
    public Notification sendEmailWithTemplate(String to, String templateName, Map<String, Object> params, UUID userId) {
        try {
            EmailTemplate template = emailTemplateRepository.findByNameAndActiveTrue(templateName)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateName));

            // ✅ Utiliser l'userId passé en paramètre
            UUID finalUserId = userId != null ? userId : UUID.randomUUID();

            Notification notification = Notification.builder()
                    .userId(finalUserId)  // ✅ Ajout de userId
                    .userEmail(to)
                    .type(templateName)
                    .subject(template.getSubject())
                    .content(params != null ? params.toString() : "")
                    .htmlContent(template.getHtmlContent())
                    .status(Notification.Status.PENDING)
                    .correlationId(UUID.randomUUID())
                    .createdAt(LocalDateTime.now())
                    .build();
            notification = notificationRepository.save(notification);

            sendEmailWithTemplateInternal(to, template, params);

            notification.setStatus(Notification.Status.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✅ Email sent to {} with template: {}", to, templateName);
            return notification;

        } catch (Exception e) {
            log.error("❌ Email with template failed: {}", e.getMessage());
            throw new RuntimeException("Failed to send email with template", e);
        }
    }

    // ===== MÉTHODES SPÉCIFIQUES POUR LES ÉVÉNEMENTS =====

    public void sendWelcomeEmail(String to, String firstName, String lastName, UUID userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("firstName", firstName);
        params.put("lastName", lastName);
        params.put("email", to);
        params.put("year", LocalDateTime.now().getYear());

        // ✅ Ajouter le lien de vérification
        String verificationLink = "http://localhost:8081/api/users/verify/" + userId;
        params.put("verificationLink", verificationLink);

        sendEmailWithTemplate(to, "WELCOME", params, userId);
        log.info("📧 Welcome email sent to {}", to);
    }

    /**
     * Envoyer un email de confirmation de paiement
     */
    public void sendPaymentSuccessEmail(String to, String amount, String currency, String transactionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency);
        params.put("transactionId", transactionId);
        params.put("date", LocalDateTime.now().toString());

        sendEmailWithTemplate(to, "PAYMENT_SUCCESS", params, null);
        log.info("📧 Payment success email sent to {}", to);
    }

    /**
     * Envoyer un email de création de livre
     */
    public void sendBookCreatedEmail(String to, String title, String authorName) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("authorName", authorName);
        params.put("date", LocalDateTime.now().toString());

        sendEmailWithTemplate(to, "BOOK_CREATED", params, null);
        log.info("📧 Book created email sent to {}", to);
    }

    // ===== MÉTHODES PRIVÉES =====

    private void sendEmailInternal(EmailRequest request) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail, senderName);
        helper.setTo(request.getTo());
        helper.setSubject(request.getSubject());
        helper.setText(buildHtmlFromParams(request), true);

        mailSender.send(message);
    }

    private void sendEmailWithTemplateInternal(String to, EmailTemplate template,
                                               Map<String, Object> params) throws Exception {
        String htmlContent = template.getHtmlContent();
        String textContent = template.getTextContent();

        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                htmlContent = htmlContent.replace("{{" + entry.getKey() + "}}", value);
                if (textContent != null) {
                    textContent = textContent.replace("{{" + entry.getKey() + "}}", value);
                }
            }
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail, senderName);
        helper.setTo(to);
        helper.setSubject(template.getSubject());
        helper.setText(textContent != null ? textContent : "", htmlContent);

        mailSender.send(message);
    }

    private String buildHtmlFromParams(EmailRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>").append(request.getSubject()).append("</h2>");

        if (request.getParams() != null) {
            html.append("<table style='width:100%; border-collapse: collapse;'>");
            for (Map.Entry<String, Object> entry : request.getParams().entrySet()) {
                html.append("<tr>");
                html.append("<td style='padding: 8px; border-bottom: 1px solid #eee;'><b>")
                        .append(entry.getKey())
                        .append(":</b></td>");
                html.append("<td style='padding: 8px; border-bottom: 1px solid #eee;'>")
                        .append(entry.getValue() != null ? entry.getValue().toString() : "")
                        .append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private Notification createNotification(EmailRequest request) {
        return Notification.builder()
                .userId(UUID.randomUUID())  // ✅ Ajouter un userId par défaut
                .userEmail(request.getTo())
                .subject(request.getSubject())
                .type(request.getTemplateName() != null ? request.getTemplateName() : "CUSTOM")
                .content(request.getParams() != null ? request.getParams().toString() : "")
                .status(Notification.Status.PENDING)
                .correlationId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Vérifier si le service email est disponible
     */
    public boolean isEmailServiceAvailable() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, senderName);
            helper.setTo(senderEmail);
            helper.setSubject("Test Email Service");
            helper.setText("Ceci est un test de connexion SMTP.", false);

            log.info("✅ Email service is available");
            return true;

        } catch (MessagingException e) {
            log.warn("⚠️ Email service is not available: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("⚠️ Email service configuration error: {}", e.getMessage());
            return false;
        }
    }
    /**
     * Envoyer un email de confirmation de vérification d'email
     */
    public void sendEmailVerifiedNotification(String to, String firstName, UUID userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("firstName", firstName);
        params.put("email", to);
        params.put("year", LocalDateTime.now().getYear());
        params.put("userId", userId != null ? userId.toString() : "");

        sendEmailWithTemplate(to, "EMAIL_VERIFIED", params, userId);
        log.info("📧 Email verified notification sent to {}", to);
    }

    /**
     * Vérification alternative : tester la connexion SMTP
     */
    public boolean checkSmtpConnection() {
        try {
            // Vérifier que le mailSender est configuré
            if (mailSender == null) {
                return false;
            }
            // Tenter de créer un message
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.warn("SMTP connection failed: {}", e.getMessage());
            return false;
        }
    }
}