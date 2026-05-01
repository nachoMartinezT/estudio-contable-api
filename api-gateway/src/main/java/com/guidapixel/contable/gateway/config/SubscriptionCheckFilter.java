package com.guidapixel.contable.gateway.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SubscriptionCheckFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SecretKeySpec jwtSigningKey;

    private final Map<String, Set<String>> tenantModulesCache = new ConcurrentHashMap<>();
    private long lastCacheRefresh = 0;
    private static final long CACHE_TTL_MS = 60000;

    private static final Map<String, String> ROUTE_TO_MODULE = Map.of(
            "/api/v1/clients/", "clients",
            "/api/v1/invoices/", "invoices",
            "/api/afip/", "afip",
            "/api/v1/audit/", "audit",
            "/api/v1/dashboard/", "dashboard"
    );

    public SubscriptionCheckFilter(
            @Value("${jwt.secret:ZGVmYXVsdFNlY3JldEtleUZvckRldmVsb3BtZW50UHVycG9zZXNPbmx5MTIzNDU2Nzg5MA==}") String jwtSecret,
            WebClient.Builder webClientBuilder,
            @Value("${services.auth-service.url:http://auth-service:8081}") String authUrl
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.jwtSigningKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA384");
        this.webClient = webClientBuilder.baseUrl(authUrl).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        Long tenantId = extractTenantId(authHeader.substring(7));
        if (tenantId == null) {
            return chain.filter(exchange);
        }

        String module = getModuleForPath(path);
        if (module == null) {
            return chain.filter(exchange);
        }

        return checkSubscription(tenantId, module, exchange, chain);
    }

    private Mono<Void> checkSubscription(Long tenantId, String module, ServerWebExchange exchange, GatewayFilterChain chain) {
        String cacheKey = tenantId.toString();

        if (isCacheValid()) {
            Set<String> modules = tenantModulesCache.get(cacheKey);
            if (modules != null && modules.contains(module)) {
                return chain.filter(exchange);
            }
            if (modules != null) {
                return denyAccess(exchange, module);
            }
        }

        return refreshCache()
                .then(Mono.defer(() -> {
                    Set<String> modules = tenantModulesCache.get(cacheKey);
                    if (modules != null && modules.contains(module)) {
                        return chain.filter(exchange);
                    }
                    return denyAccess(exchange, module);
                }));
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> refreshCache() {
        return webClient.get()
                .uri("/api/v1/admin/cache/refresh")
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(body -> {
                    try {
                        Map<String, Object> response = objectMapper.readValue(body, Map.class);
                        Map<String, List<String>> allSubscriptions = (Map<String, List<String>>) response.get("subscriptions");
                        tenantModulesCache.clear();
                        for (Map.Entry<String, List<String>> entry : allSubscriptions.entrySet()) {
                            tenantModulesCache.put(entry.getKey(), new HashSet<>(entry.getValue()));
                        }
                        lastCacheRefresh = System.currentTimeMillis();
                        log.info("Subscription cache refreshed for {} tenants", allSubscriptions.size());
                    } catch (Exception e) {
                        log.error("Error refreshing subscription cache: {}", e.getMessage());
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Could not refresh subscription cache: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private boolean isCacheValid() {
        return System.currentTimeMillis() - lastCacheRefresh < CACHE_TTL_MS && !tenantModulesCache.isEmpty();
    }

    private Long extractTenantId(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return claims.get("tenantId", Long.class);
        } catch (Exception e) {
            log.warn("Error extracting tenantId from JWT: {}", e.getMessage());
            return null;
        }
    }

    private String getModuleForPath(String path) {
        for (Map.Entry<String, String> entry : ROUTE_TO_MODULE.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/api/v1/admin/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui");
    }

    private Mono<Void> denyAccess(ServerWebExchange exchange, String module) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"Modulo '\" + module + \"' no habilitado. Contacta al administrador.\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
