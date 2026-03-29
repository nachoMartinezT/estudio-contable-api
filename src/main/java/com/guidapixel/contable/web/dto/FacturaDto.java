package com.guidapixel.contable.web.dto;

public class FacturaDto {
    private Double monto;
    private Long dniCliente; // Si es 0 o null, será Consumidor Final

    // Getters y Setters
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public Long getDniCliente() { return dniCliente; }
    public void setDniCliente(Long dniCliente) { this.dniCliente = dniCliente; }
}