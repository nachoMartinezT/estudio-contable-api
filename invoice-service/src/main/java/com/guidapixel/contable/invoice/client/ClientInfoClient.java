package com.guidapixel.contable.invoice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientInfoClient {

    private final RestTemplate restTemplate;

    @Value("${services.client-service.url:http://client-service:8082}")
    private String clientServiceUrl;

    @Value("${internal.api.key:${internal-api-key:}}")
    private String internalApiKey;

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getClient(Long tenantId, Long clientId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            Map<String, Object> response = restTemplate.exchange(
                    clientServiceUrl + "/api/internal/clients/" + tenantId + "/" + clientId,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("No se pudo obtener datos del cliente {} del tenant {}: {}", clientId, tenantId, e.getMessage());
            return Optional.empty();
        }
    }
}
