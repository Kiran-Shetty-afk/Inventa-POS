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
                    String email = jwtUtil.extractEmail(token);
                    String authorities = jwtUtil.extractAuthorities(token);

                    // Mutate request with user context headers for downstream microservices
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Role", authorities != null ? authorities : "")
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
