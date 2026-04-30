package com.guidapixel.contable.invoice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InvoiceItem extends BaseEntity {

    @Column(nullable = false)
    private String concepto;

    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    @ToString.Exclude
    @JsonIgnore
    private Invoice invoice;
}
