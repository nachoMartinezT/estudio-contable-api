package com.guidapixel.contable.invoice.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequest {
    private Long clientId;
    private String numeroFactura;
    private LocalDate fechaEmision;
    private List<InvoiceItemRequest> items;

    // Datos para emisi\u00f3n AFIP
    private boolean emitirAfip;
    private Integer tipoComprobante;
    private Integer puntoVenta;
    private Integer tipoDocumento;
    private Long numeroDocumento;
    private String nombreCliente;
    private String clientEmail;
    private Integer condicionIvaReceptorId;
    private Integer concepto;
    private LocalDate fechaServicioDesde;
    private LocalDate fechaServicioHasta;
    private LocalDate fechaVencimientoPago;
    private BigDecimal impTotConc;
    private BigDecimal impOpEx;
    private BigDecimal impTrib;
    private BigDecimal impIVA;
    private String monedaId;
    private BigDecimal monedaCotiz;
    private String cuitEmisor;
}
