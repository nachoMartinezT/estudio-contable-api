package com.guidapixel.contable.ledger.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recurring_fees")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecurringFee extends BaseEntity {

    @Column(nullable = false)
    private Long clientId;

    private String clientEmail;

    private String clientName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private int dayOfMonth = 1;
}
