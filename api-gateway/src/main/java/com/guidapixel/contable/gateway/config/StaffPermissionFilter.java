package com.guidapixel.contable.gateway.config;

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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StaffPermissionFilter implements GlobalFilter, Ordered {

    private final SecretKeySpec jwtSigningKey;

    private static final Map<String, String> ROUTE_TO_PERMISSION = Map.of(
            "/api/v1/clients/", "MANAGE_CLIENTS",
            "/api/v1/invoices/", "VIEW_INVOICES",
            "/api/v1/ledger/", "VIEW_INVOICES",
            "/api/v1/documents/", "MANAGE_DOCUMENTS",
            "/api/v1/dashboard/", "VIEW_DASHBOARD"
    );

    private static final Map<String, String> DENY_MESSAGES = Map.of(
            "MANAGE_CLIENTS", "No tienes permiso para gestionar clientes.",
            "VIEW_INVOICES", "No tienes permiso para ver facturas.",
            "CREATE_INVOICES", "No tienes permiso para crear facturas.",
            "MANAGE_DOCUMENTS", "No tienes permiso para gestionar documentos.",
            "VIEW_DASHBOARD", "No tienes permiso para ver el dashboard.",
            "MANAGE_STAFF", "No tienes permiso para gestionar empleados."
    );

    public StaffPermissionFilter(
            @Value("${jwt.secret:ZGVmYXVsdFNlY3JldEtleUZvckRldmVsb3BtZW50UHVycG9zZXNPbmx5MTIzNDU2Nzg5MA==}") String jwtSecret
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.jwtSigningKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA384");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/auth/") || path.startsWith("/api/internal/") || path.startsWith("/api/v1/mp/webhook/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String jwt = authHeader.substring(7);
        String role = extractRole(jwt);

        if (!"ROLE_STAFF".equals(role)) {
            return chain.filter(exchange);
        }

        String requiredPermission = getRequiredPermission(path);
        if (requiredPermission == null) {
            return chain.filter(exchange);
        }

        List<String> permissions = extractPermissions(jwt);

        if (permissions.contains("ALL") || permissions.contains(requiredPermission)) {
            return chain.filter(exchange);
        }

        if ("VIEW_INVOICES".equals(requiredPermission) && "POST".equals(exchange.getRequest().getMethod().name())) {
            if (!permissions.contains("CREATE_INVOICES")) {
                return denyAccess(exchange, "CREATE_INVOICES");
            }
            return chain.filter(exchange);
        }

        return denyAccess(exchange, requiredPermission);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPermissions(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            Object perms = claims.get("perms");
            if (perms instanceof List) {
                return (List<String>) perms;
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractRole(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String getRequiredPermission(String path) {
        for (Map.Entry<String, String> entry : ROUTE_TO_PERMISSION.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Mono<Void> denyAccess(ServerWebExchange exchange, String permission) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = DENY_MESSAGES.getOrDefault(permission, "Acceso denegado.");
        String body = "{\"error\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -99;
    }
}
