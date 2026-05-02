package com.guidapixel.contable.ledger.web.dto;

import com.guidapixel.contable.ledger.domain.model.MovementDirection;
import com.guidapixel.contable.ledger.domain.model.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMovementRequest {
    private MovementType type;
    private BigDecimal amount;
    private MovementDirection direction;
    private String description;
    private Long invoiceId;
    private LocalDate dueDate;
}
