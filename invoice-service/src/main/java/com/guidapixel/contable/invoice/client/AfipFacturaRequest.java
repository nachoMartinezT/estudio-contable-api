package com.guidapixel.contable.invoice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfipFacturaRequest {

    private Integer puntoVenta;
    private Integer tipoComprobante;
    private Integer tipoDocumento;
    private Long numeroDocumento;
    private String nombreCliente;
    private Integer condicionIvaReceptorId;
    private Integer concepto;
    private LocalDate fechaEmision;
    private LocalDate fechaServicioDesde;
    private LocalDate fechaServicioHasta;
    private LocalDate fechaVencimientoPago;
    private BigDecimal impTotal;
    private BigDecimal impTotConc;
    private BigDecimal impNeto;
    private BigDecimal impOpEx;
    private BigDecimal impTrib;
    private BigDecimal impIVA;
    private String monedaId;
    private BigDecimal monedaCotiz;
    private String cuitEmisor;
    private List<AfipItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AfipItemRequest {
        private String concepto;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal iva;
    }
}
