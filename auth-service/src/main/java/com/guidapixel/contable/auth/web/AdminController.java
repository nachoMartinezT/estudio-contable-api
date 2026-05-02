package com.guidapixel.contable.auth.web;

import com.guidapixel.contable.auth.service.AdminService;
import com.guidapixel.contable.auth.web.dto.AdminSubscriptionRequest;
import com.guidapixel.contable.auth.web.dto.CreateTenantRequest;
import com.guidapixel.contable.auth.web.dto.CreateUserRequest;
import com.guidapixel.contable.auth.web.dto.UpdateTenantAfipConfigRequest;
import com.guidapixel.contable.auth.web.dto.UpdateTenantMpConfigRequest;
import com.guidapixel.contable.auth.web.dto.TenantMpConfigResponse;
import com.guidapixel.contable.shared.model.TenantAfipConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/tenants")
    public ResponseEntity<?> createTenant(@RequestBody CreateTenantRequest request) {
        try {
            return ResponseEntity.ok(adminService.createTenant(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants")
    public ResponseEntity<?> getAllTenants() {
        try {
            return ResponseEntity.ok(adminService.getAllTenants());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<?> getTenantWithSubscriptions(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getTenantWithSubscriptions(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tenants/{id}/subscription")
    public ResponseEntity<?> updateSubscription(
            @PathVariable Long id,
            @RequestBody AdminSubscriptionRequest request
    ) {
        try {
            return ResponseEntity.ok(adminService.updateSubscription(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tenants/{id}/subscriptions")
    public ResponseEntity<?> updateAllSubscriptions(
            @PathVariable Long id,
            @RequestBody AdminSubscriptionRequest request
    ) {
        try {
            return ResponseEntity.ok(adminService.updateAllSubscriptions(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tenants/{id}/afip-config")
    public ResponseEntity<?> updateAfipConfig(
            @PathVariable Long id,
            @RequestBody UpdateTenantAfipConfigRequest request
    ) {
        try {
            return ResponseEntity.ok(adminService.updateAfipConfig(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/afip-config")
    public ResponseEntity<?> getTenantAfipConfig(@PathVariable Long id) {
        try {
            TenantAfipConfig config = adminService.getTenantAfipConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/tenants/{id}/cert")
    public ResponseEntity<?> uploadCert(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(adminService.uploadCert(id, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            return ResponseEntity.ok(adminService.getDashboardStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cache/refresh")
    public ResponseEntity<?> refreshSubscriptionCache() {
        try {
            return ResponseEntity.ok(adminService.getAllTenantSubscriptions());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tenants/{id}/mp-config")
    public ResponseEntity<?> updateMpConfig(
            @PathVariable Long id,
            @RequestBody UpdateTenantMpConfigRequest request
    ) {
        try {
            return ResponseEntity.ok(adminService.updateMpConfig(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/tenants/{id}/mp-config")
    public ResponseEntity<?> getTenantMpConfig(@PathVariable Long id) {
        try {
            TenantMpConfigResponse config = adminService.getTenantMpConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PutMapping("/tenants/{id}/overdue-config")
    public ResponseEntity<?> updateOverdueConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request
    ) {
        try {
            Boolean enabled = request.get("overdueReminderEnabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "overdueReminderEnabled es requerido"));
            }
            adminService.updateOverdueReminderEnabled(id, enabled);
            return ResponseEntity.ok(Map.of("status", "OK", "overdueReminderEnabled", enabled));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
