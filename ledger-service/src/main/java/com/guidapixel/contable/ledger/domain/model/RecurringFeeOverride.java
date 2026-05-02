package com.guidapixel.contable.ledger.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recurring_fee_overrides")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecurringFeeOverride extends BaseEntity {

    @Column(nullable = false)
    private Long recurringFeeId;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal overrideAmount;

    private String reason;
}
