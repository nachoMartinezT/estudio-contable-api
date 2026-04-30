package com.guidapixel.contable.dashboard.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long cantidadClientes;
    private BigDecimal totalFacturado;
    private List ultimosMovimientos;
}
