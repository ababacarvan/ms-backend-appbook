package com.msbackendappbook.contentservice.service;

import com.msbackendappbook.contentservice.exception.ResourceNotFoundException;
import com.msbackendappbook.contentservice.model.dto.AuthorDTO;
import com.msbackendappbook.contentservice.model.entity.Author;
import com.msbackendappbook.contentservice.repository.AuthorRepository;
import com.msbackendappbook.contentservice.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final FileStorageService fileStorageService;
    private final SlugGenerator slugGenerator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public AuthorDTO createAuthor(AuthorDTO authorDTO, MultipartFile profilePicture) {
        Author author = Author.builder()
                .firstName(authorDTO.getFirstName())
                .lastName(authorDTO.getLastName())
                .biography(authorDTO.getBiography())
                .country(authorDTO.getCountry())
                .email(authorDTO.getEmail())
                .website(authorDTO.getWebsite())
                .slug(slugGenerator.generateSlug(
                        authorDTO.getFirstName() + "-" + authorDTO.getLastName()
                ))
                .active(true)
                .build();

        // Upload de la photo de profil
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String fileName = fileStorageService.uploadCover(profilePicture);
                author.setProfilePictureUrl("/api/files/covers/" + fileName);
                log.info("✅ Profile picture uploaded: {}", fileName);
            } catch (Exception e) {
                log.error("❌ Failed to upload profile picture: {}", e.getMessage());
                throw new RuntimeException("Failed to upload profile picture: " + e.getMessage());
            }
        }

        author = authorRepository.save(author);

        // Envoyer événement Kafka
        try {
            kafkaTemplate.send("author-events", Map.of(
                    "eventType", "AUTHOR_CREATED",
                    "authorId", author.getId().toString(),
                    "fullName", author.getFullName(),
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to send Kafka event: {}", e.getMessage());
        }

        return mapToDTO(author);
    }

    public Page<AuthorDTO> getAllAuthors(Pageable pageable) {
        return authorRepository.findByActiveTrue(pageable)
                .map(this::mapToDTO);
    }

    public AuthorDTO getAuthorById(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
        return mapToDTO(author);
    }

    public AuthorDTO getAuthorBySlug(String slug) {
        Author author = authorRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
        return mapToDTO(author);
    }

    public Page<AuthorDTO> searchAuthors(String keyword, Pageable pageable) {
        return authorRepository.searchAuthors(keyword, pageable)
                .map(this::mapToDTO);
    }

    @Transactional
    public AuthorDTO updateAuthor(UUID id, AuthorDTO authorDTO, MultipartFile profilePicture) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        author.setFirstName(authorDTO.getFirstName());
        author.setLastName(authorDTO.getLastName());
        author.setBiography(authorDTO.getBiography());
        author.setCountry(authorDTO.getCountry());
        author.setEmail(authorDTO.getEmail());
        author.setWebsite(authorDTO.getWebsite());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            // Supprimer l'ancienne photo si elle existe
            if (author.getProfilePictureUrl() != null) {
                String oldFileName = extractFileNameFromUrl(author.getProfilePictureUrl());
                fileStorageService.deleteFile(oldFileName, "cover");
            }
            String fileName = fileStorageService.uploadCover(profilePicture);
            author.setProfilePictureUrl("/api/files/covers/" + fileName);
        }

        author = authorRepository.save(author);

        log.info("✅ Author updated: {} (ID: {})", author.getFullName(), author.getId());
        return mapToDTO(author);
    }

    @Transactional
    public void deleteAuthor(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        author.setActive(false);
        authorRepository.save(author);

        // Supprimer la photo de profil
        if (author.getProfilePictureUrl() != null) {
            String fileName = extractFileNameFromUrl(author.getProfilePictureUrl());
            fileStorageService.deleteFile(fileName, "cover");
        }

        log.info("🗑️ Author deleted: {} (ID: {})", author.getFullName(), author.getId());
    }

    // ===== MÉTHODES PRIVÉES =====

    private String extractFileNameFromUrl(String url) {
        if (url == null) return null;
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    private AuthorDTO mapToDTO(Author author) {
        return AuthorDTO.builder()
                .id(author.getId())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .fullName(author.getFullName())
                .biography(author.getBiography())
                .country(author.getCountry())
                .profilePictureUrl(author.getProfilePictureUrl())
                .email(author.getEmail())
                .website(author.getWebsite())
                .slug(author.getSlug())
                .active(author.isActive())
                .createdAt(author.getCreatedAt())
                .updatedAt(author.getUpdatedAt())
                .build();
    }
}