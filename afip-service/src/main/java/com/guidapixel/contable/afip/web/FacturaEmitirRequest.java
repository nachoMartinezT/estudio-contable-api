package com.guidapixel.contable.afip.web;

import com.guidapixel.contable.afip.web.dto.FacturaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaEmitirRequest {
    private Long tenantId;
    private FacturaDto factura;
}
