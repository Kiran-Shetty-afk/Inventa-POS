package com.zosh.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class ServiceTokenProvider {
    private final SecretKey key;

    public ServiceTokenProvider(
            @Value("${INTERNAL_SERVICE_JWT_SECRET:${JWT_SECRET}}") String internalServiceSecret,
            @Value("${JWT_SECRET}") String userJwtSecret,
            @Value("${spring.profiles.active:}") String activeProfiles) {
        if (internalServiceSecret == null || internalServiceSecret.isBlank()) {
            throw new IllegalStateException("An internal service signing key is required");
        }
        if (hasProductionProfile(activeProfiles) && internalServiceSecret.equals(userJwtSecret)) {
            throw new IllegalStateException("Production requires a dedicated internal service signing key");
        }
        this.key = Keys.hmacShaKeyFor(internalServiceSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String authorizationHeader() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer("inventa-pos-internal")
                .subject("order-sales-service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .claim("email", "order-sales-service")
                .claim("authorities", "ROLE_ORDER_SERVICE")
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    private boolean hasProductionProfile(String activeProfiles) {
        if (activeProfiles == null) return false;
        for (String profile : activeProfiles.split(",")) {
            if ("prod".equals(profile.trim())) return true;
        }
        return false;
    }
}
