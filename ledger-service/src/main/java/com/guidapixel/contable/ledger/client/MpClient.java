package com.guidapixel.contable.ledger.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MpClient {

    private final RestTemplate restTemplate;

    @Value("${services.mp-service.url:http://localhost:8089}")
    private String mpServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    public Map<String, Object> createPaymentLink(Long tenantId, Long clientId, Long movementId, BigDecimal amount, String description) {
        String url = mpServiceUrl + "/api/internal/mp/create-payment-link";

        Map<String, Object> body = Map.of(
                "tenantId", tenantId,
                "clientId", clientId,
                "movementId", movementId,
                "amount", amount,
                "description", description
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error creando payment link de MP para movimiento {}: {}", movementId, e.getMessage());
        }

        return null;
    }
}
