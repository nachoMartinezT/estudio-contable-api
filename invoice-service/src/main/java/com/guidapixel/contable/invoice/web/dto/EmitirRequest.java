package com.guidapixel.contable.invoice.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmitirRequest {
    private Long invoiceId;
    private Integer tipoComprobante;
    private Integer puntoVenta;
    private Integer concepto;
    private Integer tipoDocumento;
    private Long numeroDocumento;
    private String nombreCliente;
    private Integer condicionIvaReceptorId;
    private String monedaId;
    private BigDecimal monedaCotiz;
    private BigDecimal impIVA;
    private BigDecimal impTrib;
    private BigDecimal impOpEx;
    private BigDecimal impTotConc;
    private LocalDate fechaServicioDesde;
    private LocalDate fechaServicioHasta;
    private LocalDate fechaVencimientoPago;
}