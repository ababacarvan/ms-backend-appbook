package com.msbackendappbook.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbackendappbook.userservice.exceptions.ResourceNotFoundException;
import com.msbackendappbook.userservice.mapper.UserMapper;
import com.msbackendappbook.userservice.model.dto.RegisterRequest;
import com.msbackendappbook.userservice.model.dto.UserDTO;
import com.msbackendappbook.userservice.model.entity.OutboxEvent;
import com.msbackendappbook.userservice.model.event.UserEvent;
import com.msbackendappbook.userservice.model.entity.ReadingProgress;
import com.msbackendappbook.userservice.model.entity.User;
import com.msbackendappbook.userservice.model.enums.Role;
import com.msbackendappbook.userservice.model.enums.Status;
import com.msbackendappbook.userservice.repository.OutboxEventRepository;
import com.msbackendappbook.userservice.repository.ReadingProgressRepository;
import com.msbackendappbook.userservice.repository.SubscriptionRepository;
import com.msbackendappbook.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public UserDTO register(RegisterRequest request) {
        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. Créer l'utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.READER)
                .isActive(true)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // 3. Créer l'événement
        UUID eventId = UUID.randomUUID();
        UserEvent userEvent = UserEvent.builder()
                .eventId(eventId)
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .eventType("USER_CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        // 4. Sauvegarder dans l'outbox (même transaction)
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)  // ⚠️ AJOUTÉ : eventId était manquant
                    .aggregateId(savedUser.getId())
                    .eventType("USER_CREATED")
                    .payload(objectMapper.writeValueAsString(userEvent))
                    .headers(buildHeaders())  // Ajout des headers
                    .published(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for user registration: {}", eventId);

        } catch (Exception e) {
            log.error("Failed to save outbox event for user: {}", savedUser.getId(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }

        return userMapper.mapToDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(UUID id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Mettre à jour les champs
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setDateOfBirth(userDTO.getDateOfBirth());

        User updatedUser = userRepository.save(user);

        // Créer l'événement de mise à jour
        UUID eventId = UUID.randomUUID();
        UserEvent userEvent = UserEvent.builder()
                .eventId(eventId)
                .userId(id)
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .email(user.getEmail())
                .eventType("USER_UPDATED")
                .timestamp(LocalDateTime.now())
                .build();

        // Sauvegarder dans l'outbox
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)  // ⚠️ AJOUTÉ
                    .aggregateId(id)
                    .eventType("USER_UPDATED")
                    .payload(objectMapper.writeValueAsString(userEvent))
                    .headers(buildHeaders())
                    .published(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for user update: {}", eventId);

        } catch (Exception e) {
            log.error("Failed to save outbox event for user update: {}", id, e);
            throw new RuntimeException("Failed to save outbox event", e);
        }

        return userMapper.mapToDTO(updatedUser);
    }
    @Transactional
    public void updateLoginTimestamp(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Event pour login (optionnel)
        try {
            UUID eventId = UUID.randomUUID();
            UserEvent userEvent = UserEvent.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .eventType("USER_LOGIN")
                    .timestamp(LocalDateTime.now())
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId(userId)
                    .eventType("USER_LOGIN")
                    .payload(objectMapper.writeValueAsString(userEvent))
                    .headers(buildHeaders())
                    .published(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            log.warn("Failed to save login event to outbox: {}", e.getMessage());
        }
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Soft delete
        user.setActive(false);
        userRepository.save(user);

        // Créer l'événement de suppression
        UUID eventId = UUID.randomUUID();
        UserEvent userEvent = UserEvent.builder()
                .eventId(eventId)
                .userId(id)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .eventType("USER_DELETED")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId(id)
                    .eventType("USER_DELETED")
                    .payload(objectMapper.writeValueAsString(userEvent))
                    .headers(buildHeaders())
                    .published(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for user deletion: {}", eventId);

        } catch (Exception e) {
            log.error("Failed to save outbox event for user deletion: {}", id, e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    @Transactional
    public void verifyEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);

        // Event pour email vérifié
        UUID eventId = UUID.randomUUID();
        UserEvent userEvent = UserEvent.builder()
                .eventId(eventId)
                .userId(userId)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .eventType("EMAIL_VERIFIED")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId(userId)
                    .eventType("EMAIL_VERIFIED")
                    .payload(objectMapper.writeValueAsString(userEvent))
                    .headers(buildHeaders())
                    .published(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            log.error("Failed to save outbox event for email verification: {}", userId, e);
        }
    }

    public UserDTO getCurrentUser(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.mapToDTO(user);
        }
    public UserDTO getUserById(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
        return userMapper.mapToDTO(user);
    }
    public void startReading(UUID userId, UUID bookId, int totalPages) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReadingProgress progress = readingProgressRepository
                .findByUserAndBookId(user, bookId)
                .orElse(ReadingProgress.builder()
                        .user(user)
                        .bookId(bookId)
                        .totalPages(totalPages)
                        .status(Status.READING)
                        .build());

        progress.setCurrentPage(1);
        progress.setPercentageRead((1.0 / totalPages) * 100);
        progress.setLastReadAt(LocalDateTime.now());
        readingProgressRepository.save(progress);
    }
    @Transactional
    public void updateReadingProgress(UUID userId, UUID bookId, int currentPage) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReadingProgress progress = readingProgressRepository
                .findByUserAndBookId(user, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading progress not found"));

        int totalPages = progress.getTotalPages();

        double percentage = (currentPage / (double) totalPages) * 100;

        Status status = Status.READING;

        if (currentPage >= totalPages) {
            status = Status.COMPLETED;
        }

        progress.setCurrentPage(currentPage);
        progress.setPercentageRead(percentage);
        progress.setStatus(status);
        progress.setLastReadAt(LocalDateTime.now());

        readingProgressRepository.save(progress);
    }
    private String buildHeaders() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("version", "1.0");
            headers.put("source", "user-service");
            headers.put("correlationId", UUID.randomUUID().toString());
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            return "{}";
        }
    }




    /**
     * Récupérer l'ID de l'utilisateur connecté
     */
    public UUID getCurrentUserId() {
        try {
            UserDTO user = getCurrentUser();
            return user.getId();
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupérer l'email de l'utilisateur connecté
     */
    public String getCurrentUserEmail() {
        try {
            UserDTO user = getCurrentUser();
            return user.getEmail();
        } catch (Exception e) {
            log.error("Error getting current user email: {}", e.getMessage());
            return null;
        }
    }

    // Récupérer un utilisateur par son email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}
