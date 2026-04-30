package com.guidapixel.contable.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("lb://auth-service"))
                .route("client-service", r -> r
                        .path("/api/v1/clients/**")
                        .uri("lb://client-service"))
                .route("invoice-service", r -> r
                        .path("/api/v1/invoices/**")
                        .uri("lb://invoice-service"))
                .route("afip-service", r -> r
                        .path("/api/afip/**")
                        .uri("lb://afip-service"))
                .route("audit-service", r -> r
                        .path("/api/v1/audit/**")
                        .uri("lb://audit-service"))
                .route("dashboard-service", r -> r
                        .path("/api/v1/dashboard/**")
                        .uri("lb://dashboard-service"))
                .build();
    }
}
