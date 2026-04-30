package com.guidapixel.contable.afip.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer nroComprobante;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private String cae;

    @Column(nullable = false)
    private String vencimientoCae;

    private Long dniCliente;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    public Factura() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(Integer nroComprobante) { this.nroComprobante = nroComprobante; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }
    public String getVencimientoCae() { return vencimientoCae; }
    public void setVencimientoCae(String vencimientoCae) { this.vencimientoCae = vencimientoCae; }
    public Long getDniCliente() { return dniCliente; }
    public void setDniCliente(Long dniCliente) { this.dniCliente = dniCliente; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
}
