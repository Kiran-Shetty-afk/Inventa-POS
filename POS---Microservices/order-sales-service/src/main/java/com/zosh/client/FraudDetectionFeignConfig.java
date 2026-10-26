package com.zosh.client;

import com.zosh.service.ServiceTokenProvider;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/** Adds a short-lived service identity to internal ML inference requests. */
public class FraudDetectionFeignConfig {

    @Bean
    public RequestInterceptor fraudDetectionAuthorization(ServiceTokenProvider tokenProvider) {
        return requestTemplate -> {
            requestTemplate.removeHeader("Authorization");
            requestTemplate.header("Authorization", tokenProvider.authorizationHeader());
        };
    }
}
