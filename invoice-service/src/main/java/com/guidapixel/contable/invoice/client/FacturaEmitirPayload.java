package com.guidapixel.contable.invoice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaEmitirPayload {
    private Long tenantId;
    private AfipFacturaRequest factura;
}
