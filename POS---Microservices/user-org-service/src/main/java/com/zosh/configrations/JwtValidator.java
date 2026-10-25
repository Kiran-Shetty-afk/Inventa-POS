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
        String authorization = request.getHeader(JwtConstant.JWT_HEADER);
        if (authorization != null) {
            if (!authorization.startsWith("Bearer ")) {
                throw new BadCredentialsException("Invalid authorization header format");
            }
            try {
                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(authorization.substring(7)).getPayload();
                String email = claims.get("email", String.class);
                String authorities = claims.get("authorities", String.class);
                if (email == null || email.isBlank() || authorities == null) {
                    throw new BadCredentialsException("Invalid token claims");
                }
                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(email, null, auths));
            } catch (BadCredentialsException e) {
                throw e;
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid token");
            }
        }
        filterChain.doFilter(request, response);
    }
}
