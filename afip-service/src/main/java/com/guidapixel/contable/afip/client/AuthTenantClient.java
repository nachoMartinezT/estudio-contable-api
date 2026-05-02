package com.guidapixel.contable.afip.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidapixel.contable.shared.model.TenantAfipConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class AuthTenantClient {

    private final String authUrl;
    private final String internalApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AuthTenantClient(
            @Value("${services.auth-service.url:http://auth-service:8081}") String authUrl,
            @Value("${internal.api.key:defaultInternalKey}") String internalApiKey
    ) {
        this.authUrl = authUrl;
        this.internalApiKey = internalApiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public TenantAfipConfig getTenantAfipConfig(Long tenantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authUrl + "/api/internal/tenants/" + tenantId + "/afip-config"))
                    .header("X-Internal-Key", internalApiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Error obteniendo config AFIP del tenant: HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if ("ERROR".equals(root.get("status").asText())) {
                throw new RuntimeException("Error obteniendo config AFIP del tenant: " + root.get("error").asText());
            }

            return TenantAfipConfig.builder()
                    .tenantId(tenantId)
                    .afipCuit(root.get("afipCuit").asText())
                    .afipCertPassword(root.get("afipCertPassword").asText())
                    .afipCertPath(root.get("afipCertPath").asText())
                    .afipHomologacion(root.get("afipHomologacion").asBoolean())
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo config AFIP del tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("Error obteniendo configuracion AFIP del tenant: " + e.getMessage(), e);
        }
    }
}
