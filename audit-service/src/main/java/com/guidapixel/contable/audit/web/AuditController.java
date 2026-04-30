package com.guidapixel.contable.audit.web;

import com.guidapixel.contable.audit.domain.document.AuditLog;
import com.guidapixel.contable.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getMyAuditLogs() {
        return ResponseEntity.ok(auditService.getMyLogs());
    }

    @GetMapping("/latest")
    public ResponseEntity<List<AuditLog>> getLatestByTenant(@RequestParam Long tenantId) {
        return ResponseEntity.ok(auditService.getLogsByTenant(tenantId));
    }
}
