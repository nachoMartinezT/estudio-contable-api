package com.guidapixel.contable.mp.web;

import com.guidapixel.contable.mp.service.MpService;
import com.guidapixel.contable.mp.web.dto.MpWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/mp")
@RequiredArgsConstructor
public class MpWebhookController {

    private final MpService mpService;

    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<?> handleWebhook(
            @PathVariable Long tenantId,
            @RequestBody MpWebhookPayload payload,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId
    ) {
        try {
            if (!validateSignature(tenantId, signature, payload)) {
                log.warn("Firma invalida para webhook MP, tenantId={}", tenantId);
                return ResponseEntity.status(401).body(Map.of("status", "error", "error", "Invalid signature"));
            }

            if ("payment".equals(payload.getAction()) && payload.getData() != null) {
                String paymentId = payload.getData().getId();
                log.info("Webhook MP recibido: tenantId={}, paymentId={}", tenantId, paymentId);

                mpService.processWebhook(tenantId, paymentId);

                return ResponseEntity.ok(Map.of("status", "received"));
            }

            return ResponseEntity.ok(Map.of("status", "ignored"));
        } catch (Exception e) {
            log.error("Error procesando webhook MP para tenantId={}: {}", tenantId, e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error"));
        }
    }

    private boolean validateSignature(Long tenantId, String signatureHeader, MpWebhookPayload payload) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Header x-signature ausente para tenantId={}", tenantId);
            return false;
        }

        String webhookSecret = mpService.getTenantWebhookSecret(tenantId);
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Tenant {} no tiene webhook secret configurado", tenantId);
            return false;
        }

        String[] parts = signatureHeader.split(",");
        String ts = null;
        String v1 = null;

        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                if ("ts".equals(key)) {
                    ts = value;
                } else if ("v1".equals(key)) {
                    v1 = value;
                }
            }
        }

        if (ts == null || v1 == null) {
            log.warn("Formato de x-signature invalido: {}", signatureHeader);
            return false;
        }

        String manifestId = payload.getData() != null ? payload.getData().getId() : "";
        String dataToSign = ts + ":" + manifestId;

        String computedSignature = computeHmacSha256(dataToSign, webhookSecret);

        if (!MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8)
        )) {
            log.warn("Firma no coincide para tenantId={}. Esperada={}, Recibida={}",
                    tenantId, computedSignature, v1);
            return false;
        }

        return true;
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Error calculando HMAC-SHA256: {}", e.getMessage());
            throw new RuntimeException("Error computing HMAC signature", e);
        }
    }
}
