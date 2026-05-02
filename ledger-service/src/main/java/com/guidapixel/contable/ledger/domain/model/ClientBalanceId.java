package com.guidapixel.contable.ledger.domain.model;

import lombok.*;

import java.io.Serializable;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class ClientBalanceId implements Serializable {
    private Long clientId;
    private Long tenantId;
}
