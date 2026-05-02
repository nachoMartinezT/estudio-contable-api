package com.guidapixel.contable.mp.client;

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

    @Value("${notification-service.url:http://localhost:8090}")
    private String notificationServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    public void sendLinkPagoMp(String toEmail, String toName, String tenantName, Long tenantId, Map<String, String> variables) {
        send("LINK_PAGO_MP", toEmail, toName, tenantName, tenantId, variables);
    }

    public void sendPagoConfirmado(String toEmail, String toName, String tenantName, Long tenantId, Map<String, String> variables) {
        send("PAGO_CONFIRMADO", toEmail, toName, tenantName, tenantId, variables);
    }

    private void send(String templateType, String toEmail, String toName, String tenantName, Long tenantId, Map<String, String> variables) {
        String url = notificationServiceUrl + "/api/internal/notifications/send";

        Map<String, Object> body = Map.of(
                "templateType", templateType,
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
            log.info("Notificacion {} enviada a {}", templateType, toEmail);
        } catch (Exception e) {
            log.warn("Error enviando notificacion {}: {}", templateType, e.getMessage());
        }
    }
}
