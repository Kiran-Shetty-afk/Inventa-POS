package com.zosh.gateway.filter;

import com.zosh.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (validator.isSecured.test(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header");
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header format");
                }

                String token = authHeader.substring(7);
                try {
                    var claims = jwtUtil.validateAndGetClaims(token);
                    String email = claims.get("email", String.class);
                    String authorities = claims.get("authorities", String.class);
                    if (email == null || email.isBlank() || authorities == null || authorities.isBlank()) {
                        throw new IllegalArgumentException("Required identity claims are missing");
                    }

                    // Downstream services validate the signed JWT directly; discard caller-supplied identity headers.
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Email");
                                headers.remove("X-User-Role");
                            })
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access: invalid or expired token");
                }
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {
        // Filter configuration properties if needed
    }
}
