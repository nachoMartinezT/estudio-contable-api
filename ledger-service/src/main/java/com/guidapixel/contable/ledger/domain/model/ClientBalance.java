package com.guidapixel.contable.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_balances")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@IdClass(ClientBalanceId.class)
public class ClientBalance {

    @Id
    private Long clientId;

    @Id
    private Long tenantId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDebt;

    private LocalDateTime lastMovementAt;
}
