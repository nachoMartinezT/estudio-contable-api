package com.guidapixel.contable.ledger.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringFeeOverrideResponse {
    private Long id;
    private Long recurringFeeId;
    private Long clientId;
    private String yearMonth;
    private BigDecimal overrideAmount;
    private String reason;
    private LocalDateTime createdAt;
}
