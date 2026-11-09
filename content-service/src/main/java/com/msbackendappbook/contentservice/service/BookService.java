package com.msbackendappbook.contentservice.service;

import com.msbackendappbook.contentservice.exception.ResourceNotFoundException;
import com.msbackendappbook.contentservice.model.dto.*;
import com.msbackendappbook.contentservice.model.entity.Author;
import com.msbackendappbook.contentservice.model.entity.Book;
import com.msbackendappbook.contentservice.model.entity.Book.Format;
import com.msbackendappbook.contentservice.model.entity.Category;
import com.msbackendappbook.contentservice.model.entity.Tag;
import com.msbackendappbook.contentservice.repository.AuthorRepository;
import com.msbackendappbook.contentservice.repository.BookRepository;
import com.msbackendappbook.contentservice.repository.CategoryRepository;
import com.msbackendappbook.contentservice.repository.TagRepository;
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
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final FileStorageService fileStorageService;
    private final SlugGenerator slugGenerator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaEventPublisher kafkaEventPublisher;

    // ===== CRUD PRINCIPAL =====

    @Transactional
    public BookDTO createBook(BookRequest request, MultipartFile coverFile, MultipartFile bookFile) {
        // 1. Vérifier l'auteur
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        // 2. Vérifier si l'ISBN existe déjà
        if (request.getIsbn() != null && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new RuntimeException("Book with ISBN " + request.getIsbn() + " already exists");
        }

        // 3. Créer le livre
        Book book = Book.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .isbn(request.getIsbn())
                .slug(slugGenerator.generateSlug(request.getTitle()))
                .author(author)
                .publisher(request.getPublisher())
                .language(request.getLanguage())
                .format(request.getFormat())
                .pagesCount(request.getPagesCount())
                .available(request.isAvailable())
                .featured(request.isFeatured())
                .approved(request.isApproved())
                .downloadCount(0)
                .viewCount(0)
                .averageRating(0.0)
                .categories(new HashSet<>())
                .tags(new HashSet<>())
                .build();

        // 4. Upload de la couverture
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String fileName = fileStorageService.uploadCover(coverFile);
                book.setCoverImageName(fileName);
                book.setCoverImageUrl("/api/files/covers/" + fileName);
                book.setCoverImageSize(coverFile.getSize());
                book.setCoverImageContentType(coverFile.getContentType());
                log.info("✅ Cover uploaded: {}", fileName);
            } catch (Exception e) {
                log.error("❌ Failed to upload cover: {}", e.getMessage());
                throw new RuntimeException("Failed to upload cover: " + e.getMessage());
            }
        }

        // 5. Upload du fichier livre
        if (bookFile != null && !bookFile.isEmpty()) {
            try {
                String fileName = fileStorageService.uploadBook(bookFile);
                book.setFileName(fileName);
                book.setFileUrl("/api/files/books/" + fileName);
                book.setFileSize(bookFile.getSize());
                book.setFileContentType(bookFile.getContentType());
                log.info("✅ Book file uploaded: {}", fileName);
            } catch (Exception e) {
                log.error("❌ Failed to upload book file: {}", e.getMessage());
                throw new RuntimeException("Failed to upload book file: " + e.getMessage());
            }
        }

        // 6. Ajouter les catégories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Book finalBook = book;
            request.getCategoryIds().forEach(categoryId -> {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
                finalBook.getCategories().add(category);
            });
        }

        // 7. Ajouter les tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Book finalBook1 = book;
            request.getTagIds().forEach(tagId -> {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
                finalBook1.getTags().add(tag);
            });
        }

        // 8. Sauvegarder
        log.info("1 - avant save");

        book = bookRepository.save(book);

        log.info("2 - après save : {}", book.getId());

       sendBookEvent(book, "BOOK_CREATED");

        log.info("3 - après kafka");

        return mapToDTO(book);
    }

    @Transactional
    public BookDTO updateBook(UUID id, BookRequest request, MultipartFile coverFile, MultipartFile bookFile) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));

        // Mettre à jour les champs
        book.setTitle(request.getTitle());
        book.setSubtitle(request.getSubtitle());
        book.setDescription(request.getDescription());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setLanguage(request.getLanguage());
        book.setFormat(request.getFormat());
        book.setPagesCount(request.getPagesCount());
        book.setAvailable(request.isAvailable());
        book.setFeatured(request.isFeatured());

        // Mise à jour de la couverture
        if (coverFile != null && !coverFile.isEmpty()) {
            // Supprimer l'ancienne couverture
            if (book.getCoverImageName() != null) {
                fileStorageService.deleteFile(book.getCoverImageName(), "cover");
            }
            String fileName = fileStorageService.uploadCover(coverFile);
            book.setCoverImageName(fileName);
            book.setCoverImageUrl("/api/files/covers/" + fileName);
            book.setCoverImageSize(coverFile.getSize());
            book.setCoverImageContentType(coverFile.getContentType());
        }

        // Mise à jour du fichier livre
        if (bookFile != null && !bookFile.isEmpty()) {
            if (book.getFileName() != null) {
                fileStorageService.deleteFile(book.getFileName(), "book");
            }
            String fileName = fileStorageService.uploadBook(bookFile);
            book.setFileName(fileName);
            book.setFileUrl("/api/files/books/" + fileName);
            book.setFileSize(bookFile.getSize());
            book.setFileContentType(bookFile.getContentType());
        }

        // Mise à jour des catégories
        if (request.getCategoryIds() != null) {
            book.getCategories().clear();
            Book finalBook = book;
            request.getCategoryIds().forEach(categoryId -> {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
                finalBook.getCategories().add(category);
            });
        }

        // Mise à jour des tags
        if (request.getTagIds() != null) {
            book.getTags().clear();
            Book finalBook1 = book;
            request.getTagIds().forEach(tagId -> {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
                finalBook1.getTags().add(tag);
            });
        }

        book = bookRepository.save(book);

        // Envoyer événement de mise à jour
        sendBookEvent(book, "BOOK_UPDATED");

        log.info("✅ Book updated successfully: {} (ID: {})", book.getTitle(), book.getId());
        return mapToDTO(book);
    }

    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));

        // Supprimer les fichiers associés
        if (book.getCoverImageName() != null) {
            fileStorageService.deleteFile(book.getCoverImageName(), "cover");
        }
        if (book.getFileName() != null) {
            fileStorageService.deleteFile(book.getFileName(), "book");
        }

        bookRepository.delete(book);

        // Envoyer événement de suppression
        sendBookEvent(book, "BOOK_DELETED");

        log.info("🗑️ Book deleted successfully: {} (ID: {})", book.getTitle(), book.getId());
    }

    // ===== MÉTHODES DE LECTURE =====

    public Page<BookDTO> getAllBooks(Pageable pageable) {
//        return bookRepository.findByApprovedTrue(pageable)
//                .map(this::mapToDTO);
        return bookRepository.findAll(pageable).map(this::mapToDTO);
    }

    public BookDTO getBookById(UUID id) {
        Book book = bookRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        return mapToDTO(book);
    }

    public BookDTO getBookBySlug(String slug) {
        Book book = bookRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with slug: " + slug));
        return mapToDTO(book);
    }

    // ===== MÉTHODES DE RECHERCHE =====

    public Page<BookDTO> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.searchBooks(keyword, pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getBooksByCategory(UUID categoryId, Pageable pageable) {
        return bookRepository.findByCategoryId(categoryId, pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getBooksByAuthor(UUID authorId, Pageable pageable) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + authorId));
        return bookRepository.findByAuthorAndApprovedTrue(author, pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getBooksByFormat(Format format, Pageable pageable) {
        return bookRepository.findByFormatAndApprovedTrue(format, pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getBooksByLanguage(String language, Pageable pageable) {
        return bookRepository.findByLanguageAndApprovedTrue(language, pageable)
                .map(this::mapToDTO);
    }

    // ===== MÉTHODES DE CLASSEMENT =====

    public Page<BookDTO> getMostDownloaded(Pageable pageable) {
        return bookRepository.findMostDownloaded(pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getMostViewed(Pageable pageable) {
        return bookRepository.findMostViewed(pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getTopRated(Pageable pageable) {
        return bookRepository.findTopRated(pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getRecentBooks(Pageable pageable) {
        return bookRepository.findRecentBooks(pageable)
                .map(this::mapToDTO);
    }

    public Page<BookDTO> getFeaturedBooks(Pageable pageable) {
        return bookRepository.findByFeaturedTrueAndApprovedTrue(pageable)
                .map(this::mapToDTO);
    }

    // ===== MÉTHODES DE MISE À JOUR =====

    @Transactional
    public void incrementViewCount(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        book.setViewCount(book.getViewCount() + 1);
        bookRepository.save(book);
        log.debug("👁️ View count incremented for book: {}", bookId);
    }

    @Transactional
    public FileUploadResponse downloadBook(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        if (!book.isAvailable()) {
            throw new RuntimeException("Book is not available for download");
        }

        if (!book.isApproved()) {
            throw new RuntimeException("Book is not approved yet");
        }

        book.setDownloadCount(book.getDownloadCount() + 1);
        bookRepository.save(book);

        // Envoyer événement de téléchargement
        try {
            Map<String, Object> payload = Map.of(
                    "eventType", "BOOK_DOWNLOADED",
                    "bookId", book.getId().toString(),
                    "title", book.getTitle(),
                    "authorId", book.getAuthor().getId().toString(),
                    "authorName", book.getAuthor().getFullName(),
                    "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send("book-events", book.getId().toString(), payload);
        } catch (Exception e) {
            log.error("Failed to send download event: {}", e.getMessage());
        }

        log.info("⬇️ Book downloaded: {} (ID: {})", book.getTitle(), bookId);
        return FileUploadResponse.builder()
                .fileName(book.getFileName())
                .fileUrl(book.getFileUrl())
                .fileSize(book.getFileSize())
                .fileType(book.getFileContentType())
                .success(true)
                .message("Book downloaded successfully")
                .build();
    }

    // ===== MÉTHODES D'ADMINISTRATION =====

    @Transactional
    public BookDTO approveBook(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        if (book.isApproved()) {
            throw new RuntimeException("Book is already approved");
        }

        book.setApproved(true);
        book = bookRepository.save(book);

        sendBookEvent(book, "BOOK_APPROVED");
        log.info("✅ Book approved: {} (ID: {})", book.getTitle(), book.getId());
        return mapToDTO(book);
    }

    @Transactional
    public BookDTO rejectBook(UUID bookId, String reason) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        book.setApproved(false);
        book = bookRepository.save(book);

        try {
            Map<String, Object> payload = Map.of(
                    "eventType", "BOOK_REJECTED",
                    "bookId", book.getId().toString(),
                    "title", book.getTitle(),
                    "reason", reason != null ? reason : "No reason provided",
                    "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send("book-events", book.getId().toString(), payload);
        } catch (Exception e) {
            log.error("Failed to send reject event: {}", e.getMessage());
        }

        log.info("❌ Book rejected: {} (ID: {})", book.getTitle(), book.getId());
        return mapToDTO(book);
    }

    @Transactional
    public BookDTO toggleFeatured(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        book.setFeatured(!book.isFeatured());
        book = bookRepository.save(book);

        log.info("⭐ Book featured toggled: {} -> {} (ID: {})",
                book.getTitle(), book.isFeatured(), book.getId());
        return mapToDTO(book);
    }

    @Transactional
    public BookDTO toggleAvailability(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        book.setAvailable(!book.isAvailable());
        book = bookRepository.save(book);

        log.info("📚 Book availability toggled: {} -> {} (ID: {})",
                book.getTitle(), book.isAvailable(), book.getId());
        return mapToDTO(book);
    }

    // ===== MÉTHODES STATISTIQUES =====

    public long countTotalBooks() {
        return bookRepository.countApprovedBooks();
    }

    // ===== MÉTHODES PRIVÉES =====

    private void sendBookEvent(Book book, String eventType) {
        try {
            Map<String, String> headers = Map.of(
                    "eventType", eventType,
                    "source", "content-service",
                    "version", "1.0"
            );

            Map<String, Object> payload = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "bookId", book.getId().toString(),
                    "title", book.getTitle(),
                    "slug", book.getSlug(),
                    "authorId", book.getAuthor().getId().toString(),
                    "authorName", book.getAuthor().getFullName(),
                    "isbn", book.getIsbn() != null ? book.getIsbn() : "",
                    "format", book.getFormat() != null ? book.getFormat().name() : "",
                    "eventType", eventType,
                    "timestamp", LocalDateTime.now().toString()
            );

            kafkaEventPublisher.publishEvent(
                    "book-events",
                    book.getId().toString(),
                    payload,
                    headers
            );

            log.debug("📤 Book event sent: {} for book {}", eventType, book.getId());

        } catch (Exception e) {
            log.error("❌ Failed to send book event {}: {}", eventType, e.getMessage());
        }
    }

    private BookDTO mapToDTO(Book book) {
        if (book == null) return null;

        return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .subtitle(book.getSubtitle())
                .description(book.getDescription())
                .isbn(book.getIsbn())
                .slug(book.getSlug())
                .author(mapAuthorToDTO(book.getAuthor()))
                .publisher(book.getPublisher())
                .language(book.getLanguage())
                .coverImageUrl(book.getCoverImageUrl())
                .coverImageName(book.getCoverImageName())
                .coverImageSize(book.getCoverImageSize())
                .fileUrl(book.getFileUrl())
                .fileName(book.getFileName())
                .fileSize(book.getFileSize())
                .fileContentType(book.getFileContentType())
                .format(book.getFormat())
                .pagesCount(book.getPagesCount())
                .averageRating(book.getAverageRating())
                .downloadCount(book.getDownloadCount())
                .viewCount(book.getViewCount())
                .featured(book.isFeatured())
                .available(book.isAvailable())
                .approved(book.isApproved())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .categories(book.getCategories().stream()
                        .map(this::mapCategoryToDTO)
                        .collect(java.util.stream.Collectors.toSet()))
                .tags(book.getTags().stream()
                        .map(this::mapTagToDTO)
                        .collect(java.util.stream.Collectors.toSet()))
                .build();
    }

    private AuthorDTO mapAuthorToDTO(Author author) {
        if (author == null) return null;
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
                .active(author.isActive())
                .build();
    }

    private CategoryDTO mapCategoryToDTO(Category category) {
        if (category == null) return null;
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .build();
    }

    private TagDTO mapTagToDTO(Tag tag) {
        if (tag == null) return null;
        return TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }
}