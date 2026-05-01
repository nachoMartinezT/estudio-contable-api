package com.guidapixel.contable.auth.web;

import com.guidapixel.contable.auth.service.AdminService;
import com.guidapixel.contable.auth.web.dto.AdminSubscriptionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

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
}
