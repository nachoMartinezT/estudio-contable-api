package com.guidapixel.contable.domain.repository;

import com.guidapixel.contable.domain.document.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop5ByTenantIdOrderByTimestampDesc(Long tenantId);
}