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

public class JwtValidator extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // First check for gateway-forwarded headers (preferred in microservice architecture)
        String gatewayEmail = request.getHeader("X-User-Email");
        String gatewayRole = request.getHeader("X-User-Role");
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);
        Claims claims = null;

        if (jwt != null) {
            if (!jwt.startsWith("Bearer ")) {
                throw new BadCredentialsException("Invalid authorization header format");
            }
            try {
                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
                claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(jwt.substring(7)).getPayload();
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid token....");
            }
        }

        Long branchId = toLong(claims == null ? null : claims.get("branchId"));

        if (gatewayEmail != null && !gatewayEmail.isEmpty()) {
            // Trust the gateway's pre-validated headers
            List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(
                    gatewayRole != null ? gatewayRole : "");
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(gatewayEmail, null, auths);
            authentication.setDetails(branchId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            // Fallback: validate JWT directly (for direct access without gateway)
            if (claims != null) {
                String email = String.valueOf(claims.get("email"));
                String authorities = String.valueOf(claims.get("authorities"));

                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, auths);
                authentication.setDetails(branchId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
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
