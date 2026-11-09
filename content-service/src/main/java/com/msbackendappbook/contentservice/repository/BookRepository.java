package com.msbackendappbook.contentservice.repository;


import com.msbackendappbook.contentservice.model.entity.Author;
import com.msbackendappbook.contentservice.model.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    // ===== MÉTHODES DE BASE =====

    Optional<Book> findByIsbn(String isbn);
    Optional<Book> findBySlug(String slug);
    boolean existsByIsbn(String isbn);

    // ===== MÉTHODES AVEC FILTRES =====

    Page<Book> findByApprovedTrue(Pageable pageable);
    Page<Book> findByAvailableTrueAndApprovedTrue(Pageable pageable);
    Page<Book> findByAuthorAndApprovedTrue(Author author, Pageable pageable);
    Page<Book> findByFormatAndApprovedTrue(Book.Format format, Pageable pageable);
    Page<Book> findByLanguageAndApprovedTrue(String language, Pageable pageable);

    // ✅ CORRECTION : Renommer en findByFeaturedTrueAndApprovedTrue
    Page<Book> findByFeaturedTrueAndApprovedTrue(Pageable pageable);

    // ✅ AJOUT : Méthode pour les livres non approuvés
    Page<Book> findByApprovedFalse(Pageable pageable);

    // ===== MÉTHODES DE RECHERCHE =====

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN b.author a " +
            "LEFT JOIN b.categories c " +
            "LEFT JOIN b.tags t " +
            "WHERE b.approved = true AND " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.subtitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b JOIN b.categories c WHERE c.id = :categoryId AND b.approved = true")
    Page<Book> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b JOIN b.tags t WHERE t.id = :tagId AND b.approved = true")
    Page<Book> findByTagId(@Param("tagId") UUID tagId, Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b JOIN b.categories c WHERE c.id IN :categoryIds AND b.approved = true")
    Page<Book> findByCategoryIds(@Param("categoryIds") List<UUID> categoryIds, Pageable pageable);

    // ===== MÉTHODES DE CLASSEMENT =====

    @Query("SELECT b FROM Book b WHERE b.approved = true ORDER BY b.downloadCount DESC")
    Page<Book> findMostDownloaded(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.approved = true ORDER BY b.viewCount DESC")
    Page<Book> findMostViewed(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.approved = true AND b.averageRating IS NOT NULL ORDER BY b.averageRating DESC")
    Page<Book> findTopRated(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.approved = true ORDER BY b.createdAt DESC")
    Page<Book> findRecentBooks(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.approved = true ORDER BY b.updatedAt DESC")
    Page<Book> findRecentlyUpdated(Pageable pageable);

    // ===== MÉTHODES AVEC INTERVALLE DE DATES =====


    // ===== MÉTHODES STATISTIQUES =====

    @Query("SELECT COUNT(b) FROM Book b WHERE b.approved = true")
    long countApprovedBooks();

    @Query("SELECT b.format, COUNT(b) FROM Book b WHERE b.approved = true GROUP BY b.format")
    List<Object[]> countBooksByFormat();

    @Query("SELECT b.language, COUNT(b) FROM Book b WHERE b.approved = true GROUP BY b.language")
    List<Object[]> countBooksByLanguage();

    @Query("SELECT SUM(b.downloadCount) FROM Book b WHERE b.approved = true")
    Long sumDownloadCount();

    @Query("SELECT SUM(b.viewCount) FROM Book b WHERE b.approved = true")
    Long sumViewCount();

    @Query("SELECT b.author.id, b.author.firstName, b.author.lastName, COUNT(b), SUM(b.downloadCount), AVG(b.averageRating) " +
            "FROM Book b WHERE b.approved = true GROUP BY b.author.id, b.author.firstName, b.author.lastName")
    List<Object[]> getAuthorBookStats();

    // ===== MÉTHODES D'UPDATE EN MASSE =====

    @Modifying
    @Query("UPDATE Book b SET b.approved = true WHERE b.id = :id")
    void approveBook(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.approved = false WHERE b.id = :id")
    void rejectBook(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.featured = :featured WHERE b.id = :id")
    void updateFeaturedStatus(@Param("id") UUID id, @Param("featured") boolean featured);

    @Modifying
    @Query("UPDATE Book b SET b.available = :available WHERE b.id = :id")
    void updateAvailability(@Param("id") UUID id, @Param("available") boolean available);

    @Modifying
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.downloadCount = b.downloadCount + 1 WHERE b.id = :id")
    void incrementDownloadCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.averageRating = :rating WHERE b.id = :id")
    void updateAverageRating(@Param("id") UUID id, @Param("rating") Double rating);

    // ===== MÉTHODES AVEC FETCH JOIN =====

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.author " +
            "LEFT JOIN FETCH b.categories " +
            "LEFT JOIN FETCH b.tags " +
            "WHERE b.id = :id")
    Optional<Book> findByIdWithAllRelations(@Param("id") UUID id);

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.categories " +
            "LEFT JOIN FETCH b.tags " +
            "WHERE b.author.id = :authorId AND b.approved = true")
    List<Book> findByAuthorIdWithRelations(@Param("authorId") UUID authorId);

    // ===== MÉTHODES POUR ADMIN =====

    @Query("SELECT b FROM Book b WHERE b.approved = false ORDER BY b.createdAt ASC")
    Page<Book> findPendingApproval(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.downloadCount > :threshold OR b.viewCount > :thresholdViews")
    List<Book> findSuspiciousBooks(@Param("threshold") int threshold,
                                   @Param("thresholdViews") int thresholdViews);

    @Query("SELECT b FROM Book b WHERE b.fileUrl IS NULL OR b.fileUrl = ''")
    List<Book> findIncompleteBooks();

    @Query("SELECT b FROM Book b WHERE b.coverImageUrl IS NULL OR b.coverImageUrl = ''")
    List<Book> findBooksWithoutCover();
}