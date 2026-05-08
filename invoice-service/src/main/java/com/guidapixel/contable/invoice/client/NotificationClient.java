package com.guidapixel.contable.invoice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${services.notification-service.url:http://notification-service:8090}")
    private String notificationServiceUrl;

    @Value("${internal.api.key:${internal-api-key:}}")
    private String internalApiKey;

    public void sendFacturaEmitida(String toEmail, String toName, Long tenantId, Map<String, String> variables) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String url = notificationServiceUrl + "/api/internal/notifications/send";
        Map<String, Object> body = Map.of(
                "templateType", "FACTURA_EMITIDA",
                "toEmail", toEmail,
                "toName", toName != null ? toName : "Cliente",
                "tenantName", variables.getOrDefault("nombreEstudio", "Estudio"),
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
        } catch (Exception e) {
            log.warn("No se pudo enviar email de factura emitida a {}: {}", toEmail, e.getMessage());
        }
    }
}
