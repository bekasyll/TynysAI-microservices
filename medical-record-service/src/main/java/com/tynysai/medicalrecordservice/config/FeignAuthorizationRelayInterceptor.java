package com.tynysai.medicalrecordservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthorizationRelayInterceptor {
    @Bean
    public RequestInterceptor jwtRelayRequestInterceptor() {
        return (RequestTemplate template) -> {
            String header = inboundHeader();
            if (header != null) {
                template.header(HttpHeaders.AUTHORIZATION, header);
                return;
            }
            String token = currentJwtToken();
            if (token != null) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
        };
    }

    private static String inboundHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }

    private static String currentJwtToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt != null ? jwt.getTokenValue() : null;
        }
        return null;
    }
}