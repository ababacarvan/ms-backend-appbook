package com.msbackendappbook.contentservice.config;

import com.msbackendappbook.contentservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ============================================================
                        // 🔓 PUBLIC - Accessible sans authentification
                        // ============================================================

                        // Health check
                        .requestMatchers("/actuator/health").permitAll()

                        // Fichiers statiques (images, PDFs)
                        .requestMatchers("/api/files/**").permitAll()

                        // GET - Livres (lecture publique)
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers(
                                "/api/books/featured",
                                "/api/books/recent",
                                "/api/books/most-downloaded",
                                "/api/books/top-rated",
                                "/api/books/search"
                        ).permitAll()

                        // GET - Auteurs (lecture publique)
                        .requestMatchers(HttpMethod.GET, "/api/authors/**").permitAll()
                        .requestMatchers("/api/authors/search").permitAll()

                        // GET - Catégories et Tags (lecture publique)
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()

                        // Upload de fichiers (public pour le moment)
                        .requestMatchers(HttpMethod.POST, "/api/files/upload/**").permitAll()

                        // ============================================================
                        // 🔒 PROTÉGÉS - Nécessitent authentification
                        // ============================================================

                        // POST - Création de livres (AUTHOR ou ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/books").hasAnyRole("AUTHOR", "ADMIN")

                        // PUT - Modification de livres (AUTHOR ou ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyRole("AUTHOR", "ADMIN")

                        // DELETE - Suppression de livres (AUTHOR ou ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyRole("AUTHOR", "ADMIN")

                        // POST - Création d'auteurs (AUTHOR ou ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/authors").hasAnyRole("AUTHOR", "ADMIN")

                        // PUT - Modification d'auteurs (AUTHOR ou ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/authors/**").hasAnyRole("AUTHOR", "ADMIN")

                        // DELETE - Suppression d'auteurs (AUTHOR ou ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/authors/**").hasAnyRole("AUTHOR", "ADMIN")

                        // ADMIN - Approbations et gestion
                        .requestMatchers(
                                "/api/books/**/approve",
                                "/api/books/**/reject",
                                "/api/books/**/featured",
                                "/api/books/**/availability"
                        ).hasRole("ADMIN")

                        // 🔒 Tout le reste est protégé
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}