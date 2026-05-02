package com.guidapixel.contable.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DueDateReminderJob {

    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    @Value("${ledger-service.url:http://localhost:8088}")
    private String ledgerServiceUrl;

    @Scheduled(cron = "0 0 9 * * *", zone = "America/Argentina/Buenos_Aires")
    public void sendDueDateReminders() {
        log.info("Iniciando job de recordatorios de vencimientos");

        String url = ledgerServiceUrl + "/api/internal/ledger/movements/upcoming-due";

        HttpHeaders headers = new HttpHeaders();
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getBody() == null) {
                log.info("No hay movimientos proximos a vencer");
                return;
            }

            List<Map<String, Object>> movements = (List<Map<String, Object>>) response.getBody().get("movements");
            if (movements == null || movements.isEmpty()) {
                log.info("No hay movimientos proximos a vencer");
                return;
            }

            log.info("Se encontraron {} movimientos proximos a vencer", movements.size());

            for (Map<String, Object> movement : movements) {
                try {
                    String email = (String) movement.get("clientEmail");
                    String clientName = (String) movement.get("clientName");
                    String description = (String) movement.get("description");
                    String amount = movement.get("amount").toString();
                    String dueDate = (String) movement.get("dueDate");
                    String diasRestantes = movement.get("diasRestantes").toString();
                    String paymentLinkUrl = (String) movement.get("paymentLinkUrl");
                    String tenantName = (String) movement.get("tenantName");
                    Long tenantId = ((Number) movement.get("tenantId")).longValue();

                    Map<String, String> vars = Map.of(
                            "nombreEstudio", tenantName,
                            "nombreCliente", clientName,
                            "monto", amount,
                            "descripcion", description,
                            "fechaVencimiento", dueDate,
                            "diasRestantes", diasRestantes,
                            "paymentLinkUrl", paymentLinkUrl != null ? paymentLinkUrl : ""
                    );

                    notificationService.sendVencimientoProximo(vars, email, clientName, tenantName, tenantId);
                } catch (Exception e) {
                    log.error("Error enviando recordatorio para movimiento {}: {}", movement.get("id"), e.getMessage());
                }
            }

            log.info("Job de recordatorios completado");
        } catch (Exception e) {
            log.error("Error en job de recordatorios: {}", e.getMessage());
        }
    }
}
