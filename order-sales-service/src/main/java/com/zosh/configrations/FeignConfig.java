package com.zosh.configrations;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && !authHeader.isBlank()) {
                    requestTemplate.header("Authorization", authHeader);
                }
                String emailHeader = request.getHeader("X-User-Email");
                if (emailHeader != null && !emailHeader.isBlank()) {
                    requestTemplate.header("X-User-Email", emailHeader);
                }
                String roleHeader = request.getHeader("X-User-Role");
                if (roleHeader != null && !roleHeader.isBlank()) {
                    requestTemplate.header("X-User-Role", roleHeader);
                }
            }
        };
    }
}
