package com.zosh.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidatorTest {

    private final RouteValidator validator = new RouteValidator();

    @Test
    void onlyExactAuthenticationRoutesAreOpen() {
        assertFalse(validator.isSecured.test(MockServerHttpRequest.post("/auth/login").build()));
        assertTrue(validator.isSecured.test(MockServerHttpRequest.get("/auth/login-extra").build()));
    }

    @Test
    void actuatorAndBusinessRoutesRequireAuthentication() {
        assertTrue(validator.isSecured.test(MockServerHttpRequest.get("/actuator/prometheus").build()));
        assertTrue(validator.isSecured.test(MockServerHttpRequest.get("/api/orders/1").build()));
    }
}
