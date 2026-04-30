package com.guidapixel.contable.invoice.web.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequest {
    private Long clientId;
    private String numeroFactura;
    private LocalDate fechaEmision;
    private List<InvoiceItemRequest> items;
}
