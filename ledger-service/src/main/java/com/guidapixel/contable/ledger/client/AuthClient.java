package com.guidapixel.contable.ledger.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth-service.url:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    public boolean isMpEnabled(Long tenantId) {
        try {
            String url = authServiceUrl + "/api/internal/tenants/" + tenantId + "/mp-enabled";

            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, headers);
            if (response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("mpEnabled"));
            }
        } catch (Exception e) {
            log.warn("Error verificando MP enabled para tenant {}: {}", tenantId, e.getMessage());
        }
        return false;
    }

    public boolean isOverdueReminderEnabled(Long tenantId) {
        try {
            String url = authServiceUrl + "/api/internal/tenants/" + tenantId + "/overdue-reminder-enabled";

            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, headers);
            if (response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("overdueReminderEnabled"));
            }
        } catch (Exception e) {
            log.warn("Error verificando overdue reminder enabled para tenant {}: {}", tenantId, e.getMessage());
        }
        return false;
    }

    public Set<Long> getOverdueReminderEnabledTenantIds(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Set.of();
        }
        try {
            String idsParam = tenantIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            String url = authServiceUrl + "/api/internal/tenants/overdue-reminder-enabled?ids=" + idsParam;

            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, headers);
            if (response.getBody() != null) {
                List<?> idsList = (List<?>) response.getBody().get("tenantIds");
                if (idsList != null) {
                    return idsList.stream()
                            .map(id -> ((Number) id).longValue())
                            .collect(Collectors.toSet());
                }
            }
        } catch (Exception e) {
            log.warn("Error verificando overdue reminder enabled para tenants: {}", e.getMessage());
        }
        return Set.of();
    }

    public String getTenantName(Long tenantId) {
        try {
            String url = authServiceUrl + "/api/internal/tenants/" + tenantId + "/name";

            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, headers);
            if (response.getBody() != null) {
                return (String) response.getBody().get("name");
            }
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del tenant {}: {}", tenantId, e.getMessage());
        }
        return "Estudio";
    }
}
