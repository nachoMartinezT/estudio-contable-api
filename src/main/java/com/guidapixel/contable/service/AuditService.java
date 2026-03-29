package com.guidapixel.contable.service;

import com.guidapixel.contable.domain.document.AuditLog;
import com.guidapixel.contable.domain.repository.AuditLogRepository;
import com.guidapixel.contable.multitenancy.TenantContext;
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
        // 1. Obtener datos del contexto de seguridad y tenant
        Long tenantId = TenantContext.getTenantId();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Construir el documento
        AuditLog log = AuditLog.builder()
                .tenantId(tenantId)
                .username(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        // 3. Guardar en Mongo (es muy rápido)
        auditLogRepository.save(log);
        System.out.println("📝 Auditoría guardada en Mongo: " + action);
    }

    // Para ver los logs (filtrados por tenant)
    public List<AuditLog> getMyLogs() {
        Long tenantId = TenantContext.getTenantId();
        return auditLogRepository.findTop5ByTenantIdOrderByTimestampDesc(tenantId);
    }
}