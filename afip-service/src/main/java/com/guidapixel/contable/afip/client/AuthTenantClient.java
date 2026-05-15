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
            JsonNode statusNode = root.get("status");
            if (statusNode != null && "ERROR".equals(statusNode.asText())) {
                JsonNode errorNode = root.get("error");
                String errorMsg = errorNode != null ? errorNode.asText() : "Unknown error";
                throw new RuntimeException("Error obteniendo config AFIP del tenant: " + errorMsg);
            }

            String afipCuit = requireTextNode(root, "afipCuit", "CUIT de AFIP");
            String afipCertPassword = requireTextNode(root, "afipCertPassword", "password del certificado AFIP");
            String afipCertPath = requireTextNode(root, "afipCertPath", "ruta del certificado AFIP");
            boolean afipHomologacion = root.has("afipHomologacion") && root.get("afipHomologacion").asBoolean();

            return TenantAfipConfig.builder()
                    .tenantId(tenantId)
                    .afipCuit(afipCuit)
                    .afipCertPassword(afipCertPassword)
                    .afipCertPath(afipCertPath)
                    .afipHomologacion(afipHomologacion)
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo config AFIP del tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("Error obteniendo configuracion AFIP del tenant: " + e.getMessage(), e);
        }
    }

    private String requireTextNode(JsonNode root, String fieldName, String description) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            throw new RuntimeException("Configuracion AFIP incompleta: falta el campo '" + fieldName + "' (" + description + ")");
        }
        String value = node.asText();
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Configuracion AFIP incompleta: el campo '" + fieldName + "' (" + description + ") esta vacio");
        }
        return value;
    }
}
