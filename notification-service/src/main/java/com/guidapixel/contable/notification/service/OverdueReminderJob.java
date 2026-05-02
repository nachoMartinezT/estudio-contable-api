package com.guidapixel.contable.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueReminderJob {

    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    @Value("${services.ledger-service.url:http://localhost:8088}")
    private String ledgerServiceUrl;

    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    @Scheduled(cron = "0 0 9 * * MON", zone = "America/Argentina/Buenos_Aires")
    public void sendOverdueReminders() {
        log.info("Iniciando job de recordatorios de deuda vencida");

        HttpHeaders headers = new HttpHeaders();
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    ledgerServiceUrl + "/api/internal/ledger/movements/overdue",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getBody() == null) {
                log.info("No hay movimientos vencidos pendientes de recordatorio");
                return;
            }

            List<Map<String, Object>> movements = (List<Map<String, Object>>) response.getBody().get("movements");
            if (movements == null || movements.isEmpty()) {
                log.info("No hay movimientos vencidos pendientes de recordatorio");
                return;
            }

            log.info("Se encontraron {} movimientos vencidos", movements.size());

            Map<Long, List<Map<String, Object>>> movementsByTenant = movements.stream()
                    .collect(Collectors.groupingBy(m -> ((Number) m.get("tenantId")).longValue()));

            Map<Long, Map<String, String>> tenantInfo = new HashMap<>();

            for (Map<String, Object> movement : movements) {
                try {
                    Long tenantId = ((Number) movement.get("tenantId")).longValue();
                    String email = (String) movement.get("clientEmail");
                    String clientName = (String) movement.get("clientName");
                    String description = (String) movement.get("description");
                    String amount = movement.get("amount").toString();
                    String dueDate = (String) movement.get("dueDate");
                    String diasVencido = movement.get("diasVencido").toString();
                    String paymentLinkUrl = (String) movement.get("paymentLinkUrl");

                    if (email == null || email.isBlank()) {
                        log.warn("Movimiento {} sin email de cliente, saltando", movement.get("id"));
                        continue;
                    }

                    String tenantName = tenantInfo.computeIfAbsent(tenantId, this::fetchTenantName)
                            .getOrDefault("name", "Estudio");

                    Map<String, String> vars = Map.of(
                            "nombreEstudio", tenantName,
                            "nombreCliente", clientName != null ? clientName : "Cliente",
                            "monto", amount,
                            "descripcion", description != null ? description : "",
                            "fechaVencimiento", dueDate,
                            "diasVencido", diasVencido,
                            "paymentLinkUrl", paymentLinkUrl != null ? paymentLinkUrl : ""
                    );

                    notificationService.sendDeudaVencida(vars, email, clientName, tenantName, tenantId);

                    logOverdueReminder(tenantId, ((Number) movement.get("clientId")).longValue(),
                            ((Number) movement.get("id")).longValue());

                } catch (Exception e) {
                    log.error("Error enviando recordatorio de deuda vencida para movimiento {}: {}",
                            movement.get("id"), e.getMessage());
                }
            }

            for (Map.Entry<Long, List<Map<String, Object>>> entry : movementsByTenant.entrySet()) {
                try {
                    Long tenantId = entry.getKey();
                    List<Map<String, Object>> tenantMovements = entry.getValue();

                    String tenantName = tenantInfo.get(tenantId).getOrDefault("name", "Estudio");
                    String adminEmail = fetchTenantContactEmail(tenantId);

                    if (adminEmail == null || adminEmail.isBlank()) {
                        log.warn("Tenant {} sin email de contacto, saltando resumen", tenantId);
                        continue;
                    }

                    BigDecimal totalVencido = tenantMovements.stream()
                            .map(m -> new BigDecimal(m.get("amount").toString()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String listaClientesHtml = buildClientesTableHtml(tenantMovements);

                    Map<String, String> summaryVars = Map.of(
                            "nombreEstudio", tenantName,
                            "nombreAdmin", "Administrador",
                            "totalVencido", totalVencido.toPlainString(),
                            "listaClientes", listaClientesHtml
                    );

                    notificationService.sendResumenDeudaVencida(summaryVars, adminEmail, "Administrador", tenantName, tenantId);
                    log.info("Resumen semanal enviado al admin del tenant {}", tenantId);

                } catch (Exception e) {
                    log.error("Error enviando resumen semanal para tenant {}: {}", entry.getKey(), e.getMessage());
                }
            }

            log.info("Job de recordatorios de deuda vencida completado");
        } catch (Exception e) {
            log.error("Error en job de recordatorios de deuda vencida: {}", e.getMessage());
        }
    }

    private String buildClientesTableHtml(List<Map<String, Object>> movements) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> m : movements) {
            String clientName = (String) m.get("clientName");
            String amount = m.get("amount").toString();
            String diasVencido = m.get("diasVencido").toString();
            sb.append("<tr>")
              .append("<td style=\"padding:8px;border-bottom:1px solid #fecaca;color:#1e293b;font-size:14px;\">")
              .append(clientName != null ? clientName : "Cliente")
              .append("</td>")
              .append("<td style=\"padding:8px;border-bottom:1px solid #fecaca;color:#1e293b;font-size:14px;text-align:right;\">$")
              .append(amount)
              .append("</td>")
              .append("<td style=\"padding:8px;border-bottom:1px solid #fecaca;color:#dc2626;font-size:14px;\">")
              .append(diasVencido)
              .append("</td>")
              .append("</tr>");
        }
        return sb.toString();
    }

    private Map<String, String> fetchTenantName(Long tenantId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            ResponseEntity<Map> response = restTemplate.exchange(
                    authServiceUrl + "/api/internal/tenants/" + tenantId + "/name",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getBody() != null) {
                return Map.of("name", (String) response.getBody().get("name"));
            }
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del tenant {}: {}", tenantId, e.getMessage());
        }
        return Map.of("name", "Estudio");
    }

    private String fetchTenantContactEmail(Long tenantId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            ResponseEntity<Map> response = restTemplate.exchange(
                    authServiceUrl + "/api/internal/tenants/" + tenantId + "/contact-email",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getBody() != null) {
                return (String) response.getBody().get("email");
            }
        } catch (Exception e) {
            log.warn("Error obteniendo email de contacto del tenant {}: {}", tenantId, e.getMessage());
        }
        return "";
    }

    private void logOverdueReminder(Long tenantId, Long clientId, Long movementId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }

            Map<String, Object> body = Map.of(
                    "tenantId", tenantId,
                    "clientId", clientId,
                    "movementId", movementId
            );

            restTemplate.postForEntity(
                    ledgerServiceUrl + "/api/internal/ledger/overdue-reminders/log",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
        } catch (Exception e) {
            log.error("Error registrando log de recordatorio vencido: {}", e.getMessage());
        }
    }
}
