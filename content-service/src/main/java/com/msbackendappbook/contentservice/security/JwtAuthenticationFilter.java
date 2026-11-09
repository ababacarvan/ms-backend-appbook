package com.msbackendappbook.contentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * ✅ Ignorer les endpoints publics
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        String method = request.getMethod();

        // ✅ Endpoints publics (GET uniquement)
        if (method.equals("GET")) {
            // Fichiers statiques
            if (path.startsWith("/api/files/")) {
                return true;
            }
            // Livres
            if (path.startsWith("/api/books/") && !path.contains("/approve") &&
                    !path.contains("/reject") && !path.contains("/featured") &&
                    !path.contains("/availability")) {
                return true;
            }
            // Auteurs
            if (path.startsWith("/api/authors/") && !path.contains("/search")) {
                return true;
            }
            // Catégories et Tags
            if (path.startsWith("/api/categories/") || path.startsWith("/api/tags/")) {
                return true;
            }
            // Health check
            if (path.startsWith("/actuator/health")) {
                return true;
            }
        }

        // ✅ Upload de fichiers (POST)
        if (method.equals("POST") && path.startsWith("/api/files/upload/")) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ✅ Si c'est un endpoint public, on passe directement
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("❌ Missing or invalid Authorization header for: {}", request.getServletPath());
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {
            String email = jwtService.extractUsername(token);
            List<String> roles = jwtService.extractRoles(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("✅ User authenticated: {} with roles: {}", email, roles);
        } else {
            log.warn("❌ Invalid token for: {}", request.getServletPath());
        }

        filterChain.doFilter(request, response);
    }
}