package com.guidapixel.contable.ledger.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringFeeRequest {
    private BigDecimal baseAmount;
    private Boolean active;
}
