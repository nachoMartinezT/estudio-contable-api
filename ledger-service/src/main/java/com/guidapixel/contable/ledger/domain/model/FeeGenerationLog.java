package com.guidapixel.contable.ledger.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_generation_log", uniqueConstraints = {
    @UniqueConstraint(name = "uk_fee_generation_tenant_client_month", columnNames = {"tenant_id", "client_id", "year_month"})
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FeeGenerationLog extends BaseEntity {

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 1000)
    private String errorMessage;
}
