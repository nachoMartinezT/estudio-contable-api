package com.guidapixel.contable.auth.client;

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

    public void sendWelcomeEmail(String email, String nombre, String apellido, String tenantName, Long tenantId, String tempPassword, String loginUrl) {
        String url = notificationServiceUrl + "/api/internal/notifications/send";

        Map<String, String> variables = Map.of(
                "nombreEstudio", tenantName,
                "nombreUsuario", nombre + " " + apellido,
                "email", email,
                "passwordTemporal", tempPassword,
                "loginUrl", loginUrl
        );

        Map<String, Object> body = Map.of(
                "templateType", "BIENVENIDA_USUARIO",
                "toEmail", email,
                "toName", nombre + " " + apellido,
                "tenantName", tenantName,
                "tenantId", tenantId,
                "variables", variables
        );

        sendNotification(url, body);
    }

    private void sendNotification(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            log.info("Notificacion enviada a {}", body.get("toEmail"));
        } catch (Exception e) {
            log.warn("Error enviando notificacion: {}", e.getMessage());
        }
    }
}
