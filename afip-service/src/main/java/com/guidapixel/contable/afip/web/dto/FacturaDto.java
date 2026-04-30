package com.guidapixel.contable.afip.web.dto;

public class FacturaDto {
    private Double monto;
    private Long dniCliente;

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public Long getDniCliente() { return dniCliente; }
    public void setDniCliente(Long dniCliente) { this.dniCliente = dniCliente; }
}
