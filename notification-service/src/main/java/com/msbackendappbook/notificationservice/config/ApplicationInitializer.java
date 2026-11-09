package com.msbackendappbook.notificationservice.config;

import com.msbackendappbook.notificationservice.model.entity.EmailTemplate;
import com.msbackendappbook.notificationservice.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitializer {

    private final EmailTemplateRepository emailTemplateRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeEmailTemplates() {
        log.info("📧 Initialisation des templates d'emails...");

        createTemplate("WELCOME", "Bienvenue sur AppBook !", getWelcomeTemplate());
        createTemplate("BOOK_CREATED", "Votre livre a été créé avec succès !", getBookCreatedTemplate());
        createTemplate("PAYMENT_SUCCESS", "Paiement confirmé !", getPaymentSuccessTemplate());
        createTemplate("PAYMENT_FAILED", "Échec du paiement", getPaymentFailedTemplate());
        createTemplate("RESET_PASSWORD", "Réinitialisation du mot de passe", getResetPasswordTemplate());
        createTemplate("EMAIL_VERIFIED", "Email vérifié avec succès !", getEmailVerifiedTemplate());
        createTemplate("PROFILE_UPDATED", "Votre profil a été mis à jour", getProfileUpdatedTemplate());
        createTemplate("BOOK_APPROVED", "Votre livre a été approuvé !", getBookApprovedTemplate());
        createTemplate("SUBSCRIPTION_CREATED", "Abonnement créé avec succès !", getSubscriptionCreatedTemplate());
        createTemplate("ACCOUNT_ACTIVATED", "Votre compte est activé !", getAccountActivatedTemplate());
        createTemplate("NEW_DEVICE_LOGIN", "Nouvelle connexion détectée", getNewDeviceLoginTemplate());
        log.info("✅ Tous les templates d'emails sont initialisés !");
    }

    private void createTemplate(String name, String subject, String htmlContent) {
        try {
            if (!emailTemplateRepository.existsByName(name)) {
                EmailTemplate template = new EmailTemplate();
                template.setName(name);
                template.setSubject(subject);
                template.setHtmlContent(htmlContent);
                template.setActive(true);
                template.setCreatedAt(LocalDateTime.now());
                template.setUpdatedAt(LocalDateTime.now());

                emailTemplateRepository.save(template);
                log.info("✅ Template '{}' créé avec succès", name);
            } else {
                log.info("⏭️ Template '{}' existe déjà", name);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du template '{}': {}", name, e.getMessage());
        }
    }

    // ===== TEMPLATES HTML =====

    private String getWelcomeTemplate() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Bienvenue sur AppBook</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }
                .container { background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .header { text-align: center; border-bottom: 2px solid #4A90D9; padding-bottom: 20px; }
                .header h1 { color: #4A90D9; margin: 0; }
                .content { padding: 20px 0; }
                .btn { display: inline-block; background-color: #4A90D9; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; }
                .btn:hover { background-color: #357ABD; }
                .footer { margin-top: 30px; font-size: 12px; color: #888; text-align: center; border-top: 1px solid #eee; padding-top: 20px; }
                .warning { background-color: #fff3cd; padding: 10px 15px; border-radius: 5px; color: #856404; margin: 15px 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>📚 Bienvenue sur AppBook !</h1>
                </div>
                <div class="content">
                    <h2>Bonjour {{firstName}} {{lastName}} !</h2>
                    <p>Nous sommes ravis de vous accueillir sur <strong>AppBook</strong>, la plateforme de lecture numérique.</p>
                    <p><strong>Email :</strong> {{email}}</p>
                    
                    <div class="warning">
                        <p>⚠️ <strong>Votre compte n'est pas encore activé.</strong></p>
                        <p>Pour activer votre compte, cliquez sur le bouton ci-dessous :</p>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="{{verificationLink}}" class="btn">✅ Activer mon compte</a>
                    </div>
                    
                    <p style="font-size: 14px; color: #666;">
                        Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                        <a href="{{verificationLink}}">{{verificationLink}}</a>
                    </p>
                    <p style="font-size: 14px; color: #666;">
                        Ce lien expire dans <strong>24 heures</strong>.
                    </p>
                </div>
                <div class="footer">
                    <p>© {{year}} AppBook. Tous droits réservés.</p>
                    <p>Cet email a été envoyé automatiquement. Merci de ne pas y répondre.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }

    private String getBookCreatedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Livre créé</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #27AE60; text-align: center;">📖 Votre livre a été créé !</h1>
                    <p><strong>Titre :</strong> {{title}}</p>
                    <p><strong>Auteur :</strong> {{authorName}}</p>
                    <p><strong>ID :</strong> {{bookId}}</p>
                    <p><strong>Date :</strong> {{date}}</p>
                </div>
            </body>
            </html>
            """;
    }

    private String getPaymentSuccessTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Paiement réussi</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #2ECC71; text-align: center;">✅ Paiement confirmé !</h1>
                    <p><strong>Montant :</strong> {{amount}} {{currency}}</p>
                    <p><strong>Transaction :</strong> {{transactionId}}</p>
                    <p><strong>Date :</strong> {{date}}</p>
                </div>
            </body>
            </html>
            """;
    }

    private String getPaymentFailedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Échec du paiement</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #E74C3C; text-align: center;">❌ Échec du paiement</h1>
                    <p><strong>Montant :</strong> {{amount}} {{currency}}</p>
                    <p><strong>Transaction :</strong> {{transactionId}}</p>
                    <p><strong>Date :</strong> {{date}}</p>
                </div>
            </body>
            </html>
            """;
    }

    private String getResetPasswordTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Réinitialisation</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #E74C3C; text-align: center;">🔐 Réinitialisation du mot de passe</h1>
                    <h2>Bonjour {{firstName}} {{lastName}},</h2>
                    <p>Cliquez sur le lien pour réinitialiser votre mot de passe :</p>
                    <p><a href="{{resetLink}}">🔑 Réinitialiser</a></p>
                    <p>Ce lien expire dans {{expiryTime}} minutes.</p>
                </div>
            </body>
            </html>
            """;
    }

    private String getEmailVerifiedTemplate() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Email vérifié</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }
                .container { background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .header { text-align: center; border-bottom: 2px solid #2ECC71; padding-bottom: 20px; }
                .header h1 { color: #2ECC71; margin: 0; }
                .content { padding: 20px 0; }
                .btn { display: inline-block; background-color: #2ECC71; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; }
                .btn:hover { background-color: #27AE60; }
                .footer { margin-top: 30px; font-size: 12px; color: #888; text-align: center; border-top: 1px solid #eee; padding-top: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>✅ Email vérifié !</h1>
                </div>
                <div class="content">
                    <h2>Bonjour {{firstName}},</h2>
                    <p>Votre adresse email <strong>{{email}}</strong> a été vérifiée avec succès.</p>
                    <p>🎉 <strong>Votre compte est maintenant activé !</strong></p>
                    
                    <p>Vous pouvez dès maintenant :</p>
                    <ul>
                        <li>📖 Explorer notre catalogue de livres</li>
                        <li>❤️ Ajouter des livres à vos favoris</li>
                        <li>📝 Laisser des avis et des commentaires</li>
                    </ul>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="https://yourappbook.com/login" class="btn">🚀 Se connecter</a>
                    </div>
                </div>
                <div class="footer">
                    <p>© {{year}} AppBook. Tous droits réservés.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }

    private String getProfileUpdatedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Profil mis à jour</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #4A90D9; text-align: center;">👤 Profil mis à jour</h1>
                    <p>Bonjour {{firstName}},</p>
                    <p>Votre profil a été mis à jour avec succès.</p>
                </div>
            </body>
            </html>
            """;
    }

    private String getBookApprovedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Livre approuvé</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #27AE60; text-align: center;">✅ Votre livre a été approuvé !</h1>
                    <p><strong>Titre :</strong> {{title}}</p>
                    <p><strong>ID :</strong> {{bookId}}</p>
                </div>
            </body>
            </html>
            """;
    }

    private String getSubscriptionCreatedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Abonnement créé</title></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h1 style="color: #4A90D9; text-align: center;">🎉 Abonnement créé !</h1>
                    <p><strong>Type :</strong> {{planType}}</p>
                    <p><strong>Début :</strong> {{startDate}}</p>
                    <p><strong>Fin :</strong> {{endDate}}</p>
                </div>
            </body>
            </html>
            """;
    }
    private String getAccountActivatedTemplate() {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>Compte activé</title></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
            <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <h1 style="color: #27AE60; text-align: center;">🎉 Compte activé !</h1>
                <p>Bonjour {{firstName}},</p>
                <p>Votre compte AppBook est maintenant <strong>actif</strong>.</p>
                <p>Vous pouvez vous connecter : <a href="https://yourappbook.com/login">Se connecter</a></p>
            </div>
        </body>
        </html>
        """;
    }

    private String getNewDeviceLoginTemplate() {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>Nouvelle connexion</title></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
            <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <h1 style="color: #E67E22; text-align: center;">🔔 Nouvelle connexion</h1>
                <p>Bonjour {{firstName}},</p>
                <p>Une nouvelle connexion a été détectée sur votre compte.</p>
                <p><strong>Appareil :</strong> {{device}}</p>
                <p><strong>IP :</strong> {{ipAddress}}</p>
                <p><strong>Date :</strong> {{date}}</p>
                <p style="color: #E74C3C;">Si vous n'êtes pas à l'origine de cette connexion, changez immédiatement votre mot de passe.</p>
            </div>
        </body>
        </html>
        """;
    }
}