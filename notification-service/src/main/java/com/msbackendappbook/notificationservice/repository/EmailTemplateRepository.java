package com.msbackendappbook.notificationservice.repository;

import com.msbackendappbook.notificationservice.model.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    Optional<EmailTemplate> findByNameAndActiveTrue(String name);

    boolean existsByName(String name);


}