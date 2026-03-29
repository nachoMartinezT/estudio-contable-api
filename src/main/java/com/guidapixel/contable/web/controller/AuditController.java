package com.guidapixel.contable.web.controller;

import com.guidapixel.contable.domain.document.AuditLog;
import com.guidapixel.contable.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}