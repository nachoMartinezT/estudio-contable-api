package com.guidapixel.contable.afip.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas_afip")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Factura extends BaseEntity {

    // Identificaci\u00f3n del comprobante
    @Column(nullable = false)
    private Integer puntoVenta;

    @Column(nullable = false)
    private Integer tipoComprobante;

    @Column(nullable = false)
    private Integer nroComprobante;

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
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal impTotConc;

    @Column(precision = 12, scale = 2)
    private BigDecimal impNeto;

    @Column(precision = 12, scale = 2)
    private BigDecimal impOpEx;

    @Column(precision = 12, scale = 2)
    private BigDecimal impTrib;

    @Column(precision = 12, scale = 2)
    private BigDecimal impIVA;

    // Moneda
    private String monedaId;
    private BigDecimal monedaCotiz;

    // Datos del emisor
    @Column(nullable = false)
    private String cuitEmisor;

    // CAE y autorizaci\u00f3n
    @Column(nullable = false)
    private String cae;

    @Column(nullable = false)
    private LocalDate vencimientoCae;

    private String resultado;
    private String observaciones;

    // Relaci\u00f3n con factura interna (opcional)
    private Long invoiceId;
}
