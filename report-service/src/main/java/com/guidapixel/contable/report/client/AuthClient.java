package com.guidapixel.contable.report.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

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
