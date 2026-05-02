package com.guidapixel.contable.ledger.web.dto;

import com.guidapixel.contable.ledger.domain.model.MovementDirection;
import com.guidapixel.contable.ledger.domain.model.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResponse {
    private Long id;
    private Long clientId;
    private MovementType type;
    private BigDecimal amount;
    private MovementDirection direction;
    private String description;
    private Long invoiceId;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String mpPreferenceId;
    private String mpPaymentLinkUrl;
    private String mpStatus;
}
