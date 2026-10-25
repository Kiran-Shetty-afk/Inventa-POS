package com.zosh.configrations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class JwtValidator extends OncePerRequestFilter {
    private static final String INTERNAL_PATH = "/api/inventories/internal/stock-operations";
    private static final String INTERNAL_ISSUER = "inventa-pos-internal";
    private static final String INTERNAL_SUBJECT = "order-sales-service";
    private static final String INTERNAL_AUTHORITY = "ROLE_ORDER_SERVICE";

    private final String jwtSecret;
    private final String internalServiceSecret;

    public JwtValidator(String jwtSecret, String internalServiceSecret, String activeProfiles) {
        if (jwtSecret == null || jwtSecret.isBlank() || internalServiceSecret == null || internalServiceSecret.isBlank()) {
            throw new IllegalStateException("JWT signing keys are required");
        }
        boolean production = activeProfiles != null && Arrays.stream(activeProfiles.split(","))
                .map(String::trim).anyMatch("prod"::equals);
        if (production && jwtSecret.equals(internalServiceSecret)) {
            throw new IllegalStateException("Production requires a dedicated internal service signing key");
        }
        this.jwtSecret = jwtSecret;
        this.internalServiceSecret = internalServiceSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(JwtConstant.JWT_HEADER);
        if (authorization != null) {
            if (!authorization.startsWith("Bearer ")) {
                throw new BadCredentialsException("Invalid authorization header format");
            }
            try {
                boolean internalEndpoint = "POST".equalsIgnoreCase(request.getMethod())
                        && INTERNAL_PATH.equals(request.getServletPath());
                String verificationSecret = internalEndpoint ? internalServiceSecret : jwtSecret;
                SecretKey key = Keys.hmacShaKeyFor(verificationSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(authorization.substring(7)).getPayload();
                String email = claims.get("email", String.class);
                String authorities = claims.get("authorities", String.class);
                if (email == null || email.isBlank() || authorities == null) {
                    throw new BadCredentialsException("Invalid token claims");
                }
                if (internalEndpoint) {
                    if (!INTERNAL_ISSUER.equals(claims.getIssuer())
                            || !INTERNAL_SUBJECT.equals(claims.getSubject())
                            || !INTERNAL_AUTHORITY.equals(authorities)) {
                        throw new BadCredentialsException("Invalid internal service token");
                    }
                } else if (INTERNAL_ISSUER.equals(claims.getIssuer())
                        || Arrays.asList(authorities.split(",")).contains(INTERNAL_AUTHORITY)) {
                    throw new BadCredentialsException("Internal service token is restricted to its stock operation endpoint");
                }
                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, auths);
                authentication.setDetails(toLong(claims.get("branchId")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BadCredentialsException e) {
                throw e;
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid token");
            }
        }
        filterChain.doFilter(request, response);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                throw new BadCredentialsException("Invalid branch claim in token");
            }
        }
        return null;
    }
}
