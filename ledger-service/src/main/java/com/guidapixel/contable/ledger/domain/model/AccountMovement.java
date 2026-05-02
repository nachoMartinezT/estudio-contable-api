package com.guidapixel.contable.ledger.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_movements")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountMovement extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long clientId;

    private String clientEmail;

    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementDirection direction;

    @Column(nullable = false)
    private String description;

    private Long invoiceId;

    private LocalDate dueDate;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private Long createdByUserId;

    private String mpPreferenceId;

    private String mpPaymentLinkUrl;

    private String mpStatus;
}
