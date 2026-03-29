package com.guidapixel.contable.web.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequest {
    private Long clientId; // ID del cliente al que le facturamos
    private String numeroFactura;
    private LocalDate fechaEmision;
    private List<InvoiceItemRequest> items; // Lista de renglones
}