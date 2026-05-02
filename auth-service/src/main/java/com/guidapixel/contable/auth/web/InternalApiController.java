package com.guidapixel.contable.auth.web;

import com.guidapixel.contable.auth.domain.model.Role;
import com.guidapixel.contable.auth.service.AdminService;
import com.guidapixel.contable.auth.web.dto.CreateUserRequest;
import com.guidapixel.contable.shared.model.TenantAfipConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final AdminService adminService;

    @GetMapping("/tenants/{id}/afip-config")
    public ResponseEntity<?> getTenantAfipConfig(@PathVariable Long id) {
        try {
            TenantAfipConfig config = adminService.getTenantAfipConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/tenants/{tenantId}/client-users")
    public ResponseEntity<?> createClientUser(
            @PathVariable Long tenantId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String email = (String) request.get("email");
            String nombre = (String) request.get("nombre");
            String apellido = (String) request.get("apellido");
            Number clientIdNum = (Number) request.get("clientId");

            CreateUserRequest userRequest = CreateUserRequest.builder()
                    .email(email)
                    .nombre(nombre)
                    .apellido(apellido)
                    .role(Role.CLIENT)
                    .clientId(clientIdNum != null ? clientIdNum.longValue() : null)
                    .build();

            return ResponseEntity.ok(adminService.createUser(tenantId, userRequest, tenantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/mp-access-token")
    public ResponseEntity<?> getTenantMpAccessToken(@PathVariable Long id) {
        try {
            String token = adminService.getDecryptedMpAccessToken(id);
            return ResponseEntity.ok(Map.of("accessToken", token, "tenantId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/mp-webhook-secret")
    public ResponseEntity<?> getTenantMpWebhookSecret(@PathVariable Long id) {
        try {
            String secret = adminService.getDecryptedMpWebhookSecret(id);
            return ResponseEntity.ok(Map.of("webhookSecret", secret, "tenantId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/mp-enabled")
    public ResponseEntity<?> isTenantMpEnabled(@PathVariable Long id) {
        try {
            boolean enabled = adminService.isMpEnabled(id);
            return ResponseEntity.ok(Map.of("mpEnabled", enabled, "tenantId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/name")
    public ResponseEntity<?> getTenantName(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getTenantName(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/overdue-reminder-enabled")
    public ResponseEntity<?> isOverdueReminderEnabled(@PathVariable Long id) {
        try {
            boolean enabled = adminService.isOverdueReminderEnabled(id);
            return ResponseEntity.ok(Map.of("overdueReminderEnabled", enabled, "tenantId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/contact-email")
    public ResponseEntity<?> getTenantContactEmail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getTenantContactEmail(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/overdue-reminder-enabled")
    public ResponseEntity<?> getOverdueReminderEnabledTenantIds(@RequestParam String ids) {
        try {
            Set<Long> tenantIds = java.util.Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toSet());
            Set<Long> enabled = adminService.getOverdueReminderEnabledTenantIds(tenantIds);
            return ResponseEntity.ok(Map.of("tenantIds", enabled));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
