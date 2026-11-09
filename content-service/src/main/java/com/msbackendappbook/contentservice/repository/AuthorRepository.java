package com.msbackendappbook.contentservice.repository;

import com.msbackendappbook.contentservice.model.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {

    /**
     * Rechercher un auteur par son slug
     */
    Optional<Author> findBySlug(String slug);

    /**
     * Rechercher un auteur par son email
     */
    Optional<Author> findByEmail(String email);

    /**
     * Vérifier si un email existe déjà
     */
    boolean existsByEmail(String email);

    /**
     * Récupérer tous les auteurs actifs (paginated)
     */
    Page<Author> findByActiveTrue(Pageable pageable);

    /**
     * Récupérer tous les auteurs actifs (liste)
     */
    List<Author> findByActiveTrue();

    /**
     * Rechercher des auteurs par nom ou prénom
     */
    @Query("SELECT a FROM Author a WHERE " +
            "LOWER(a.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CONCAT(a.firstName, ' ', a.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Author> searchAuthors(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Rechercher des auteurs par pays
     */
    List<Author> findByCountryIgnoreCase(String country);

    /**
     * Récupérer les auteurs les plus populaires (avec le plus de livres)
     */
    @Query("SELECT a FROM Author a JOIN a.books b WHERE a.active = true " +
            "GROUP BY a.id ORDER BY COUNT(b) DESC")
    Page<Author> findMostPopularAuthors(Pageable pageable);

    /**
     * Récupérer les auteurs par date de création (récent)
     */
    @Query("SELECT a FROM Author a WHERE a.active = true ORDER BY a.createdAt DESC")
    Page<Author> findRecentAuthors(Pageable pageable);

    /**
     * Compter le nombre de livres d'un auteur
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.author.id = :authorId AND b.approved = true")
    long countBooksByAuthor(@Param("authorId") UUID authorId);

    /**
     * Récupérer les auteurs avec leurs livres (fetch join pour optimisation)
     */
    @Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") UUID id);

    /**
     * Récupérer les auteurs ayant un livre dans une catégorie spécifique
     */
    @Query("SELECT DISTINCT a FROM Author a JOIN a.books b JOIN b.categories c WHERE c.id = :categoryId AND a.active = true")
    Page<Author> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Récupérer les auteurs avec une biographie non vide
     */
    @Query("SELECT a FROM Author a WHERE a.biography IS NOT NULL AND a.biography != '' AND a.active = true")
    Page<Author> findAuthorsWithBiography(Pageable pageable);

    /**
     * Mettre à jour le statut actif d'un auteur
     */
    @Modifying
    @Query("UPDATE Author a SET a.active = :active WHERE a.id = :id")
    void updateActiveStatus(@Param("id") UUID id, @Param("active") boolean active);
}