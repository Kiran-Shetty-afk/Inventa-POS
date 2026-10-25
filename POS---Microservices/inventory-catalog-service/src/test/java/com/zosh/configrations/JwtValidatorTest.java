package com.zosh.configrations;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtValidatorTest {
    private static final String USER_KEY = "local-user-signing-key-32-bytes-minimum";
    private static final String INTERNAL_KEY = "separate-internal-signing-key-32-bytes";

    @Test
    void dedicatedInternalTokenIsAcceptedOnlyForStockOperationEndpoint() throws Exception {
        JwtValidator validator = new JwtValidator(USER_KEY, INTERNAL_KEY, "");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/inventories/internal/stock-operations");
        request.setServletPath("/api/inventories/internal/stock-operations");
        request.addHeader("Authorization", "Bearer " + internalToken(INTERNAL_KEY));
        AtomicBoolean continued = new AtomicBoolean(false);

        validator.doFilterInternal(request, new MockHttpServletResponse(), chain(continued));

        assertTrue(continued.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    void normalJwtCannotAuthorizeInternalStockOperation() {
        JwtValidator validator = new JwtValidator(USER_KEY, INTERNAL_KEY, "");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/inventories/internal/stock-operations");
        request.setServletPath("/api/inventories/internal/stock-operations");
        request.addHeader("Authorization", "Bearer " + token(USER_KEY, "user-app", "ROLE_ADMIN"));

        assertThrows(BadCredentialsException.class, () ->
                validator.doFilterInternal(request, new MockHttpServletResponse(), chain(new AtomicBoolean())));
    }

    @Test
    void internalTokenCannotBeReusedOnAnotherApiPath() {
        JwtValidator validator = new JwtValidator(USER_KEY, INTERNAL_KEY, "");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("Authorization", "Bearer " + internalToken(INTERNAL_KEY));

        assertThrows(BadCredentialsException.class, () ->
                validator.doFilterInternal(request, new MockHttpServletResponse(), chain(new AtomicBoolean())));
    }

    @Test
    void productionRequiresASeparateInternalKey() {
        assertThrows(IllegalStateException.class, () -> new JwtValidator(USER_KEY, USER_KEY, "prod"));
    }

    private FilterChain chain(AtomicBoolean continued) {
        return (request, response) -> continued.set(true);
    }

    private String internalToken(String secret) {
        return Jwts.builder()
                .issuer("inventa-pos-internal")
                .subject("order-sales-service")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .claim("email", "order-sales-service")
                .claim("authorities", "ROLE_ORDER_SERVICE")
                .signWith(key(secret))
                .compact();
    }

    private String token(String secret, String subject, String authority) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .claim("email", subject)
                .claim("authorities", authority)
                .signWith(key(secret))
                .compact();
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
