package com.guidapixel.contable.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "audit_logs") // Nombre de la "tabla" en Mongo
public class AuditLog {

    @Id // El ID en Mongo es un String raro (ObjectId)
    private String id;

    private Long tenantId;    // Para filtrar logs por estudio
    private String username;  // El culpable
    private String action;    // Ej: "CREATE_INVOICE"
    private String entityName; // Ej: "Invoice"
    private String entityId;   // Ej: "1" (ID de la factura)
    private String details;    // Descripción legible

    private LocalDateTime timestamp;
}