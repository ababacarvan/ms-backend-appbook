package com.msbackendappbook.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    // ✅ Liste des chemins publics (exacts)
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/users/login",
            "/api/users/register",
            "/api/users/verify",
            "/api/users/refresh",
            "/api/auth/login",
            "/api/auth/register",
            "/api/books",
            "/api/books/featured",
            "/api/books/recent",
            "/api/books/most-downloaded",
            "/api/books/top-rated",
            "/actuator/health",
            "/actuator/info",
            "/fallback"
    );

    // ✅ Préfixes publics (tous les chemins qui commencent par)
    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/api/files/",        // ✅ AJOUTÉ : fichiers statiques (images, PDFs)
            "/api/books/",         // ✅ AJOUTÉ : tous les livres (GET)
            "/api/authors/",       // ✅ AJOUTÉ : tous les auteurs (GET)
            "/api/categories/",    // ✅ AJOUTÉ : toutes les catégories (GET)
            "/api/tags/",          // ✅ AJOUTÉ : tous les tags (GET)
            "/api/users/login",
            "/api/users/register",
            "/api/users/verify",
            "/api/auth/",
            "/api/payments/webhook",
            "/api/subscriptions/plans",
            "/fallback/",
            "/actuator/"
    );

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.debug("➡ {} {}", method, path);

        // ✅ 1. OPTIONS (CORS preflight) - autoriser directement
        if ("OPTIONS".equals(method)) {
            log.debug("🔓 OPTIONS request for: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        // ✅ 2. Routes publiques
        if (isPublic(path)) {
            log.debug("🔓 Public path: {}", path);
            return chain.filter(exchange);
        }

        // ✅ 3. Vérifier le token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("❌ Missing token for: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);

            log.debug("✅ Authenticated: {} (ID: {}, Role: {})", username, userId, role);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User", username != null ? username : "unknown")
                    .header("X-User-Id", userId != null ? userId : "unknown")
                    .header("X-User-Role", role != null ? role : "unknown")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("❌ JWT invalid: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublic(String path) {
        // ✅ Vérifier les chemins exacts
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath)) {
                return true;
            }
        }

        // ✅ Vérifier les préfixes
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        // ✅ Vérifier les fichiers statiques
        if (path.matches("/api/files/covers/.*") || path.matches("/api/files/books/.*")) {
            return true;
        }

        return false;
    }

    @Override
    public int getOrder() {
        return -2; // Priorité plus haute
    }
}