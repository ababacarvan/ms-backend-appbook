package com.msbackendappbook.contentservice.controller;

import com.msbackendappbook.contentservice.model.dto.AuthorDTO;
import com.msbackendappbook.contentservice.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    // ✅ POST avec JSON (pour le frontend)
    @PostMapping
    public ResponseEntity<AuthorDTO> createAuthor(@Valid @RequestBody AuthorDTO authorDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authorService.createAuthor(authorDTO, null));
    }

    // ✅ POST avec multipart pour upload de photo (chemin différent)
    @PostMapping(value = "/with-file", consumes = {"multipart/form-data"})
    public ResponseEntity<AuthorDTO> createAuthorWithFile(
            @Valid @RequestPart("author") AuthorDTO authorDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authorService.createAuthor(authorDTO, profilePicture));
    }

    @GetMapping
    public ResponseEntity<Page<AuthorDTO>> getAllAuthors(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(authorService.getAllAuthors(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorDTO> getAuthorById(@PathVariable UUID id) {
        return ResponseEntity.ok(authorService.getAuthorById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AuthorDTO>> searchAuthors(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(authorService.searchAuthors(keyword, pageable));
    }

    @PutMapping(value = "/{id}/with-file", consumes = {"multipart/form-data"})
    public ResponseEntity<AuthorDTO> updateAuthorWithFile(
            @PathVariable UUID id,
            @Valid @RequestPart("author") AuthorDTO authorDTO,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        return ResponseEntity.ok(authorService.updateAuthor(id, authorDTO, profilePicture));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorDTO> updateAuthor(
            @PathVariable UUID id,
            @Valid @RequestBody AuthorDTO authorDTO) {
        return ResponseEntity.ok(authorService.updateAuthor(id, authorDTO, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok().build();
    }
}