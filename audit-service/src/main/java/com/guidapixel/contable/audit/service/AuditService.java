package com.guidapixel.contable.audit.service;

import com.guidapixel.contable.audit.domain.document.AuditLog;
import com.guidapixel.contable.audit.domain.repository.AuditLogRepository;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String action, String entityName, String entityId, String details) {
        Long tenantId = TenantContext.getTenantId();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        AuditLog log = AuditLog.builder()
                .tenantId(tenantId)
                .username(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    public List<AuditLog> getMyLogs() {
        Long tenantId = TenantContext.getTenantId();
        return auditLogRepository.findTop10ByTenantIdOrderByTimestampDesc(tenantId);
    }

    public List<AuditLog> getLogsByTenant(Long tenantId) {
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId);
    }
}
