package com.guidapixel.contable.auth.web;

import com.guidapixel.contable.auth.service.AdminService;
import com.guidapixel.contable.auth.web.dto.CreateUserRequest;
import com.guidapixel.contable.auth.web.dto.StaffPermissionsResponse;
import com.guidapixel.contable.auth.web.dto.UpdateStaffPermissionsRequest;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantUserController {

    private final AdminService adminService;

    @PostMapping("/{tenantId}/users")
    public ResponseEntity<?> createUser(
            @PathVariable Long tenantId,
            @RequestBody CreateUserRequest request
    ) {
        try {
            Long requesterTenantId = TenantContext.getTenantId();
            return ResponseEntity.ok(adminService.createUser(tenantId, request, requesterTenantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PutMapping("/{tenantId}/users/{userId}/permissions")
    public ResponseEntity<?> updateStaffPermissions(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @RequestBody UpdateStaffPermissionsRequest request
    ) {
        try {
            Long requesterTenantId = TenantContext.getTenantId();
            StaffPermissionsResponse response = adminService.updateStaffPermissions(tenantId, userId, request, requesterTenantId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/{tenantId}/users/{userId}/permissions")
    public ResponseEntity<?> getStaffPermissions(
            @PathVariable Long tenantId,
            @PathVariable Long userId
    ) {
        try {
            Long requesterTenantId = TenantContext.getTenantId();
            if (!tenantId.equals(requesterTenantId)) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No tienes permiso para ver permisos de otro tenant"));
            }
            return ResponseEntity.ok(adminService.getStaffPermissions(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffUsers() {
        try {
            Long tenantId = TenantContext.getTenantId();
            return ResponseEntity.ok(adminService.getStaffUsers(tenantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
