package com.guidapixel.contable.audit.domain.repository;

import com.guidapixel.contable.audit.domain.document.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop5ByTenantIdOrderByTimestampDesc(Long tenantId);
    List<AuditLog> findTop10ByTenantIdOrderByTimestampDesc(Long tenantId);
    List<AuditLog> findByTenantIdOrderByTimestampDesc(Long tenantId);
}
