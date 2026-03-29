package com.guidapixel.contable.web.dto;

import com.guidapixel.contable.domain.document.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class DashboardResponse {
    private long cantidadClientes;
    private BigDecimal totalFacturado;
    private List<AuditLog> ultimosMovimientos;
}