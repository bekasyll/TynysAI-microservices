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
                        .path("/api/users/**", "/api/doctors/**", "/api/patients/**", "/api/admin/**")
                        .uri("lb://user-service"))
                .route("appointment-service", r -> r
                        .path("/api/appointments/**")
                        .uri("lb://appointment-service"))
                .route("medical-record-service", r -> r
                        .path("/api/reports/**", "/api/lab-results/**")
                        .uri("lb://medical-record-service"))
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .uri("lb://notification-service"))
                .route("xray-service", r -> r
                        .path("/api/xrays/**")
                        .uri("lb://xray-service"))
                .build();
    }
}