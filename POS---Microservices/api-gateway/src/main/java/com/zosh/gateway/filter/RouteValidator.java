package com.zosh.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/auth/signup",
            "/auth/login",
            "/auth/signin",
            "/auth/reset-password",
            "/auth/forgot-password",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().normalize().getPath();
                return OPEN_API_ENDPOINTS.stream().noneMatch(prefix ->
                        path.equals(prefix) || path.startsWith(prefix + "/"));
            };
}
