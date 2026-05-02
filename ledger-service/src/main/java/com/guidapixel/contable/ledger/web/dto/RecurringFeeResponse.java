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
public class RecurringFeeResponse {
    private Long id;
    private Long clientId;
    private BigDecimal baseAmount;
    private boolean active;
    private int dayOfMonth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
