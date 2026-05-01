package com.guidapixel.contable.afip.web.dto;

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
public class FacturaDto {

    // Identificaci\u00f3n del comprobante
    private Integer puntoVenta;
    private Integer tipoComprobante;

    // Datos del receptor
    private Integer tipoDocumento;
    private Long numeroDocumento;
    private String nombreCliente;
    private Integer condicionIvaReceptorId;

    // Concepto y fechas
    private Integer concepto;
    private LocalDate fechaEmision;
    private LocalDate fechaServicioDesde;
    private LocalDate fechaServicioHasta;
    private LocalDate fechaVencimientoPago;

    // Importes
    private BigDecimal impTotal;
    private BigDecimal impTotConc;
    private BigDecimal impNeto;
    private BigDecimal impOpEx;
    private BigDecimal impTrib;
    private BigDecimal impIVA;

    // Moneda
    private String monedaId;
    private BigDecimal monedaCotiz;

    // Datos del emisor (opcional, usa default del tenant si no se especifica)
    private String cuitEmisor;

    // Items de la factura (para c\u00e1lculo autom\u00e1tico de importes)
    private List<ItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private String concepto;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal iva;
    }
}
