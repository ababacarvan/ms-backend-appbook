package com.msbackendappbook.contentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbackendappbook.contentservice.model.dto.BookDTO;
import com.msbackendappbook.contentservice.model.dto.BookRequest;
import com.msbackendappbook.contentservice.model.dto.FileUploadResponse;
import com.msbackendappbook.contentservice.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;
    private final ObjectMapper objectMapper;

    // ============================================================
    // 🔓 PUBLIC
    // ============================================================

    @GetMapping
    public ResponseEntity<Page<BookDTO>> getAllBooks(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/featured")
    public ResponseEntity<Page<BookDTO>> getFeaturedBooks(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getFeaturedBooks(pageable));
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<BookDTO>> getRecentBooks(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getRecentBooks(pageable));
    }

    @GetMapping("/most-downloaded")
    public ResponseEntity<Page<BookDTO>> getMostDownloaded(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getMostDownloaded(pageable));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<Page<BookDTO>> getTopRated(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getTopRated(pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<BookDTO>> getBooksByCategory(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId, pageable));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<BookDTO>> getBooksByAuthor(
            @PathVariable UUID authorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByAuthor(authorId, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookDTO>> searchBooks(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooks(keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable UUID id) {
        bookService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileUploadResponse> downloadBook(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        log.info("📥 User {} is downloading book {}", userId, id);
        return ResponseEntity.ok(bookService.downloadBook(id));
    }

    // ============================================================
    // 🔒 AUTHOR - Création de livre avec fichiers
    // ============================================================

    // ✅ UNIQUE méthode POST pour la création
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('AUTHOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createBook(

            @RequestPart("book") String bookJson,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "bookFile", required = false) MultipartFile bookFile) {
        try {
            BookRequest request = objectMapper.readValue(bookJson, BookRequest.class);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUserId = auth.getName();

            log.info("📝 User {} is creating a new book: {}", currentUserId, request.getTitle());

            BookDTO book = bookService.createBook(request, coverFile, bookFile);
            return ResponseEntity.status(HttpStatus.CREATED).body(book);

        } catch (Exception e) {
            log.error("❌ Error creating book: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('AUTHOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateBook(
            @PathVariable UUID id,
            @RequestPart("book") String bookJson,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "bookFile", required = false) MultipartFile bookFile) {
        try {
            BookRequest request = objectMapper.readValue(bookJson, BookRequest.class);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUserId = auth.getName();

            log.info("✏️ User {} is updating book {}", currentUserId, id);
            BookDTO book = bookService.updateBook(id, request, coverFile, bookFile);
            return ResponseEntity.ok(book);

        } catch (Exception e) {
            log.error("❌ Error updating book: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AUTHOR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = auth.getName();
        log.info("🗑️ User {} is deleting book {}", currentUserId, id);
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 🔒 ADMIN
    // ============================================================

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> approveBook(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        log.info("✅ Admin {} approved book {}", adminId, id);
        return ResponseEntity.ok(bookService.approveBook(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> rejectBook(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        log.info("❌ Admin {} rejected book {} for reason: {}", adminId, id, reason);
        return ResponseEntity.ok(bookService.rejectBook(id, reason));
    }

    @PostMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> toggleFeatured(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        log.info("⭐ Admin {} toggled featured for book {}", adminId, id);
        return ResponseEntity.ok(bookService.toggleFeatured(id));
    }

    @PostMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookDTO> toggleAvailability(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        log.info("📚 Admin {} toggled availability for book {}", adminId, id);
        return ResponseEntity.ok(bookService.toggleAvailability(id));
    }
}