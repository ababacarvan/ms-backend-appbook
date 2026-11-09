package com.msbackendappbook.contentservice.repository;

import com.msbackendappbook.contentservice.model.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /**
     * Rechercher un tag par son slug
     */
    Optional<Tag> findBySlug(String slug);

    /**
     * Rechercher un tag par son nom
     */
    Optional<Tag> findByName(String name);

    /**
     * Vérifier si un nom de tag existe déjà
     */
    boolean existsByName(String name);

    /**
     * Vérifier si un slug de tag existe déjà
     */
    boolean existsBySlug(String slug);

    /**
     * Récupérer les tags les plus populaires (avec le plus de livres)
     */
    @Query("SELECT t FROM Tag t JOIN t.books b WHERE b.approved = true " +
            "GROUP BY t.id ORDER BY COUNT(b) DESC")
    Page<Tag> findMostPopularTags(Pageable pageable);

    /**
     * Rechercher des tags par nom (pour autocomplete)
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Tag> searchTags(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Récupérer les tags d'un livre
     */
    @Query("SELECT t FROM Tag t JOIN t.books b WHERE b.id = :bookId")
    List<Tag> findTagsByBookId(@Param("bookId") UUID bookId);

    /**
     * Compter le nombre de livres pour chaque tag
     */
    @Query("SELECT t, COUNT(b) FROM Tag t LEFT JOIN t.books b WHERE b.approved = true GROUP BY t.id")
    List<Object[]> findAllWithBookCount();

    /**
     * Récupérer les tags avec leurs livres (pour optimisation)
     */
    @Query("SELECT DISTINCT t FROM Tag t LEFT JOIN FETCH t.books WHERE t.id = :id")
    Optional<Tag> findByIdWithBooks(@Param("id") UUID id);

    /**
     * Récupérer les tags par liste de noms
     */
    List<Tag> findByNameIn(List<String> names);

    /**
     * Récupérer les tags recommandés (tags les plus utilisés avec une certaine catégorie)
     */
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.books b JOIN b.categories c " +
            "WHERE c.id = :categoryId AND b.approved = true " +
            "ORDER BY SIZE(t.books) DESC")
    Page<Tag> findRecommendedTagsByCategory(@Param("categoryId") UUID categoryId, Pageable pageable);
}