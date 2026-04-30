package com.guidapixel.contable.audit.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private Long tenantId;
    private String username;
    private String action;
    private String entityName;
    private String entityId;
    private String details;

    private LocalDateTime timestamp;
}
