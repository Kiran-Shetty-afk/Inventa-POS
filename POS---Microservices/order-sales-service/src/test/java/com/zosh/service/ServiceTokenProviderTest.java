package com.zosh.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceTokenProviderTest {
    private static final String INTERNAL_KEY = "separate-internal-signing-key-32-bytes";
    private static final String USER_KEY = "local-user-signing-key-32-bytes-minimum";

    @Test
    void createsShortLivedTokenWithNarrowInternalIdentity() {
        ServiceTokenProvider provider = new ServiceTokenProvider(INTERNAL_KEY, USER_KEY, "prod");
        String header = provider.authorizationHeader();
        String token = header.substring("Bearer ".length());
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(INTERNAL_KEY.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();

        assertEquals("inventa-pos-internal", claims.getIssuer());
        assertEquals("order-sales-service", claims.getSubject());
        assertEquals("ROLE_ORDER_SERVICE", claims.get("authorities", String.class));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(USER_KEY.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token));
    }

    @Test
    void productionCannotReuseTheUserJwtSigningKeyForInternalCalls() {
        assertThrows(IllegalStateException.class,
                () -> new ServiceTokenProvider(USER_KEY, USER_KEY, "prod"));
    }
}
