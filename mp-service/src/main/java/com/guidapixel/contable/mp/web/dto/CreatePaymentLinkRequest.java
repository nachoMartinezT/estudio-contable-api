package com.guidapixel.contable.mp.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentLinkRequest {
    private Long tenantId;
    private Long clientId;
    private Long movementId;
    private BigDecimal amount;
    private String description;
}
