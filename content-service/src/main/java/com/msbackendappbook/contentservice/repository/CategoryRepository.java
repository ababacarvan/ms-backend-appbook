package com.msbackendappbook.contentservice.repository;

import com.msbackendappbook.contentservice.model.entity.Category;
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
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Rechercher une catégorie par son slug
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Rechercher une catégorie par son nom
     */
    Optional<Category> findByName(String name);

    /**
     * Vérifier si un nom de catégorie existe déjà
     */
    boolean existsByName(String name);

    /**
     * Vérifier si un slug de catégorie existe déjà
     */
    boolean existsBySlug(String slug);

    /**
     * Récupérer toutes les catégories parentes (sans parent)
     */
    List<Category> findByParentIsNull();

    /**
     * Récupérer les sous-catégories d'une catégorie parente
     */
    List<Category> findByParentId(UUID parentId);

    /**
     * Récupérer les sous-catégories d'une catégorie parente (paginated)
     */
    Page<Category> findByParentId(UUID parentId, Pageable pageable);

    /**
     * Récupérer toutes les catégories avec leurs livres
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.books WHERE c.id = :id")
    Optional<Category> findByIdWithBooks(@Param("id") UUID id);

    /**
     * Compter le nombre de livres dans une catégorie (incluant les sous-catégories)
     */
    @Query("SELECT COUNT(DISTINCT b) FROM Category c " +
            "JOIN c.books b " +
            "WHERE c.id = :categoryId AND b.approved = true")
    long countBooksInCategory(@Param("categoryId") UUID categoryId);

    /**
     * Compter le nombre de livres dans une catégorie et ses sous-catégories
     */
    @Query("SELECT COUNT(DISTINCT b) FROM Category c " +
            "JOIN c.books b " +
            "WHERE (c.id = :categoryId OR c.parent.id = :categoryId) AND b.approved = true")
    long countBooksInCategoryTree(@Param("categoryId") UUID categoryId);

    /**
     * Rechercher des catégories par nom (pour autocomplete)
     */
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Category> searchCategories(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Récupérer les catégories les plus populaires (avec le plus de livres)
     */
    @Query("SELECT c FROM Category c JOIN c.books b WHERE b.approved = true " +
            "GROUP BY c.id ORDER BY COUNT(b) DESC")
    Page<Category> findMostPopularCategories(Pageable pageable);

    /**
     * Récupérer toutes les catégories avec leur nombre de livres
     */
    @Query("SELECT c, COUNT(b) FROM Category c LEFT JOIN c.books b WHERE b.approved = true GROUP BY c.id")
    List<Object[]> findAllWithBookCount();

    /**
     * Récupérer l'arbre des catégories (pour navigation)
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.parent IS NULL")
    List<Category> findRootCategoriesWithChildren();
}