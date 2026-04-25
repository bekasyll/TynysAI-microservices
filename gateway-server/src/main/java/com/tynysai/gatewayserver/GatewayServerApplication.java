package com.tynysai.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServerApplication.class, args);
    }

    @Bean
    public RouteLocator tynysaiRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/doctors/**", "/api/patients/**",
                                "/api/admin/**", "/api/auth/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("userServiceCB")
                                .setFallbackUri("forward:/fallback/user-service")))
                        .uri("lb://user-service"))
                .route("appointment-service", r -> r
                        .path("/api/appointments/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("appointmentServiceCB")
                                .setFallbackUri("forward:/fallback/appointment-service")))
                        .uri("lb://appointment-service"))
                .route("medical-record-service", r -> r
                        .path("/api/reports/**", "/api/lab-results/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("medicalRecordServiceCB")
                                .setFallbackUri("forward:/fallback/medical-record-service")))
                        .uri("lb://medical-record-service"))
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("notificationServiceCB")
                                .setFallbackUri("forward:/fallback/notification-service")))
                        .uri("lb://notification-service"))
                .route("xray-service", r -> r
                        .path("/api/xrays/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("xrayServiceCB")
                                .setFallbackUri("forward:/fallback/xray-service")))
                        .uri("lb://xray-service"))
                .route("user-service-docs", r -> r
                        .path("/v3/api-docs/user-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/user-service", "/v3/api-docs"))
                        .uri("lb://user-service"))
                .route("appointment-service-docs", r -> r
                        .path("/v3/api-docs/appointment-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/appointment-service", "/v3/api-docs"))
                        .uri("lb://appointment-service"))
                .route("medical-record-service-docs", r -> r
                        .path("/v3/api-docs/medical-record-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/medical-record-service", "/v3/api-docs"))
                        .uri("lb://medical-record-service"))
                .route("notification-service-docs", r -> r
                        .path("/v3/api-docs/notification-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/notification-service", "/v3/api-docs"))
                        .uri("lb://notification-service"))
                .route("xray-service-docs", r -> r
                        .path("/v3/api-docs/xray-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/xray-service", "/v3/api-docs"))
                        .uri("lb://xray-service"))
                .build();
    }
}