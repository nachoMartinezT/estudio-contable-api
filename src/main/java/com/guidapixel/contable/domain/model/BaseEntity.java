package com.guidapixel.contable.domain.model;

import com.guidapixel.contable.multitenancy.TenantContext;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@MappedSuperclass // Significa: "No crees una tabla para esto, pero sus hijos sí tendrán estas columnas"
@EntityListeners(AuditingEntityListener.class)
// 1. Definimos el filtro que el Aspecto va a activar
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
// 2. Definimos la condición SQL automática (WHERE tenant_id = X)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Esta columna es la clave del Multi-tenancy
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Magia: Antes de guardar cualquier dato (Cliente, Factura, etc),
    // si no tiene tenantId, se lo ponemos automáticamente desde el contexto.
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
    }
}