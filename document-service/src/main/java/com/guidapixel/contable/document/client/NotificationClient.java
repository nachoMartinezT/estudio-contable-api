package com.guidapixel.contable.document.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${services.notification-service.url:http://localhost:8090}")
    private String notificationServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public void sendDocumentoCompartido(String toEmail, String toName, String tenantName, Long tenantId,
                                         String nombreArchivo, String categoria, String descripcion) {
        String url = notificationServiceUrl + "/api/internal/notifications/send";

        String portalUrl = appBaseUrl + "/documentos";

        Map<String, String> variables = Map.of(
                "nombreEstudio", tenantName,
                "nombreCliente", toName,
                "nombreArchivo", nombreArchivo,
                "categoria", categoria,
                "descripcion", descripcion != null ? descripcion : "",
                "portalUrl", portalUrl
        );

        Map<String, Object> body = Map.of(
                "templateType", "DOCUMENTO_COMPARTIDO",
                "toEmail", toEmail,
                "toName", toName,
                "tenantName", tenantName,
                "tenantId", tenantId,
                "variables", variables
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            log.info("Notificacion DOCUMENTO_COMPARTIDO enviada a {}", toEmail);
        } catch (Exception e) {
            log.warn("Error enviando notificacion de documento: {}", e.getMessage());
        }
    }
}
