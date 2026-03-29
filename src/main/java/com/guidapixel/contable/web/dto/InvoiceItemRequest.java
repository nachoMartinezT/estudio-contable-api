package com.guidapixel.contable.web.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceItemRequest {
    private String concepto;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
}