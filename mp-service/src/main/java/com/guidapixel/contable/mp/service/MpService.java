package com.guidapixel.contable.mp.service;

import com.guidapixel.contable.mp.client.NotificationClient;
import com.guidapixel.contable.mp.web.dto.CreatePaymentLinkRequest;
import com.guidapixel.contable.mp.web.dto.MpPaymentDetail;
import com.guidapixel.contable.mp.web.dto.PaymentLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpService {

    private final RestTemplate restTemplate;
    private final NotificationClient notificationClient;

    @Value("${mp.base-url:https://api.mercadopago.com}")
    private String mpBaseUrl;

    @Value("${auth-service.url:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${ledger-service.url:http://ledger-service:8088}")
    private String ledgerServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    public PaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request) {
        String accessToken = getTenantAccessToken(request.getTenantId());

        Map<String, Object> preference = Map.of(
                "items", List.of(Map.of(
                        "title", request.getDescription(),
                        "quantity", 1,
                        "currency_id", "ARS",
                        "unit_price", request.getAmount()
                )),
                "external_reference", request.getMovementId().toString(),
                "back_urls", Map.of(
                        "success", "https://app.guidapixel.com/cuenta-corriente",
                        "pending", "https://app.guidapixel.com/cuenta-corriente",
                        "failure", "https://app.guidapixel.com/cuenta-corriente"
                ),
                "notification_url", getWebhookUrl(request.getTenantId())
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                mpBaseUrl + "/checkout/preferences",
                new HttpEntity<>(preference, headers),
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Error creando preferencia de MercadoPago: " + response.getBody());
        }

        Map<String, Object> body = response.getBody();
        String preferenceId = (String) body.get("id");
        String sandboxInitPoint = (String) body.get("sandbox_init_point");
        String initPoint = (String) body.get("init_point");

        String paymentLinkUrl = sandboxInitPoint != null ? sandboxInitPoint : initPoint;

        log.info("Payment link creado para movimiento {}: preferenceId={}", request.getMovementId(), preferenceId);

        sendLinkPagoMpEmail(request, paymentLinkUrl);

        return PaymentLinkResponse.builder()
                .preferenceId(preferenceId)
                .paymentLinkUrl(paymentLinkUrl)
                .build();
    }

    public void processWebhook(Long tenantId, String paymentId) {
        String accessToken = getTenantAccessToken(tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                mpBaseUrl + "/v1/payments/" + paymentId,
                Map.class,
                headers
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Error obteniendo pago {} de MP: {}", paymentId, response.getBody());
            return;
        }

        Map<String, Object> payment = response.getBody();
        String status = (String) payment.get("status");
        String preferenceId = (String) payment.get("preference_id");

        if ("approved".equals(status)) {
            notifyLedgerMarkPaid(preferenceId);
            sendPagoConfirmadoEmail(tenantId, payment);
        } else {
            log.info("Pago {} con estado {}, no se marca como pagado. tenantId={}", paymentId, status, tenantId);
        }
    }

    private String getTenantAccessToken(Long tenantId) {
        String url = authServiceUrl + "/api/internal/tenants/" + tenantId + "/mp-access-token";

        HttpHeaders headers = new HttpHeaders();
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        ResponseEntity<Map> response = restTemplate.getForEntity(
                url,
                Map.class,
                headers
        );

        Map<String, Object> body = response.getBody();
        return (String) body.get("accessToken");
    }

    private void notifyLedgerMarkPaid(String preferenceId) {
        String url = ledgerServiceUrl + "/api/internal/ledger/movements/mark-paid-by-mp";

        Map<String, Object> body = Map.of("preferenceId", preferenceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        try {
            restTemplate.put(url, new HttpEntity<>(body, headers));
            log.info("Notificado a ledger-service marcar movimiento como pagado por MP, preferenceId={}", preferenceId);
        } catch (Exception e) {
            log.error("Error notificando a ledger-service para preferenceId={}: {}", preferenceId, e.getMessage());
        }
    }

    private String getWebhookUrl(Long tenantId) {
        return "https://api.guidapixel.com/api/v1/mp/webhook/" + tenantId;
    }

    private void sendLinkPagoMpEmail(CreatePaymentLinkRequest request, String paymentLinkUrl) {
        try {
            String tenantName = getTenantName(request.getTenantId());
            Map<String, String> variables = Map.of(
                    "nombreEstudio", tenantName,
                    "nombreCliente", "",
                    "monto", request.getAmount().toString(),
                    "descripcion", request.getDescription(),
                    "fechaVencimiento", "",
                    "paymentLinkUrl", paymentLinkUrl
            );
            notificationClient.sendLinkPagoMp(
                    "", "", tenantName, request.getTenantId(), variables
            );
        } catch (Exception e) {
            log.warn("Error enviando email de link de pago: {}", e.getMessage());
        }
    }

    private void sendPagoConfirmadoEmail(Long tenantId, Map<String, Object> payment) {
        try {
            String tenantName = getTenantName(tenantId);
            Map<String, String> variables = Map.of(
                    "nombreEstudio", tenantName,
                    "nombreCliente", "",
                    "monto", payment.get("transaction_amount") != null ? payment.get("transaction_amount").toString() : "",
                    "descripcion", payment.get("description") != null ? payment.get("description").toString() : "",
                    "fechaPago", payment.get("date_approved") != null ? payment.get("date_approved").toString() : ""
            );
            notificationClient.sendPagoConfirmado(
                    "", "", tenantName, tenantId, variables
            );
        } catch (Exception e) {
            log.warn("Error enviando email de pago confirmado: {}", e.getMessage());
        }
    }

    private String getTenantName(Long tenantId) {
        try {
            String url = authServiceUrl + "/api/internal/tenants/" + tenantId + "/name";
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (response.getBody() != null) {
                return (String) response.getBody().get("name");
            }
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del tenant {}: {}", tenantId, e.getMessage());
        }
        return "Estudio";
    }

    public String getTenantWebhookSecret(Long tenantId) {
        String url = authServiceUrl + "/api/internal/tenants/" + tenantId + "/mp-webhook-secret";

        HttpHeaders headers = new HttpHeaders();
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getBody() != null) {
                return (String) response.getBody().get("webhookSecret");
            }
        } catch (Exception e) {
            log.warn("Error obteniendo webhook secret para tenant {}: {}", tenantId, e.getMessage());
        }
        return null;
    }
}
