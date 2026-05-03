package com.guidapixel.contable.report.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerClient {

    private final RestTemplate restTemplate;

    @Value("${services.ledger-service.url:http://localhost:8088}")
    private String ledgerServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAccountStatementData(Long tenantId, Long clientId, String from, String to) {
        try {
            String url = ledgerServiceUrl + "/api/internal/ledger/report-data/account-statement"
                    + "?tenantId=" + tenantId + "&clientId=" + clientId + "&from=" + from + "&to=" + to;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(internalHeaders()), Map.class);
            if (response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("movements");
            }
        } catch (Exception e) {
            log.error("Error obteniendo datos de cuenta corriente: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllMovements(Long tenantId, String from, String to) {
        try {
            String url = ledgerServiceUrl + "/api/internal/ledger/report-data/all-movements"
                    + "?tenantId=" + tenantId + "&from=" + from + "&to=" + to;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(internalHeaders()), Map.class);
            if (response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("movements");
            }
        } catch (Exception e) {
            log.error("Error obteniendo movimientos del tenant: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActiveFees(Long tenantId) {
        try {
            String url = ledgerServiceUrl + "/api/internal/ledger/report-data/active-fees?tenantId=" + tenantId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(internalHeaders()), Map.class);
            if (response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("fees");
            }
        } catch (Exception e) {
            log.error("Error obteniendo honorarios activos: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFeeGenerationLog(Long tenantId, Long clientId, String yearMonth) {
        try {
            String url = ledgerServiceUrl + "/api/internal/ledger/report-data/fee-generation-log"
                    + "?tenantId=" + tenantId + "&clientId=" + clientId + "&yearMonth=" + yearMonth;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(internalHeaders()), Map.class);
            if (response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("logs");
            }
        } catch (Exception e) {
            log.error("Error obteniendo fee generation log: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFeeOverride(Long tenantId, Long clientId, String yearMonth) {
        try {
            String url = ledgerServiceUrl + "/api/internal/ledger/report-data/fee-override"
                    + "?tenantId=" + tenantId + "&clientId=" + clientId + "&yearMonth=" + yearMonth;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(internalHeaders()), Map.class);
            if (response.getBody() != null) {
                return (Map<String, Object>) response.getBody().get("override");
            }
        } catch (Exception e) {
            log.error("Error obteniendo fee override: {}", e.getMessage());
        }
        return null;
    }
}
