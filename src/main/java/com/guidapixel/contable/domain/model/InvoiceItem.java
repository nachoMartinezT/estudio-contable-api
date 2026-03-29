package com.guidapixel.contable.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InvoiceItem extends BaseEntity {

    @Column(nullable = false)
    private String concepto; // Ej: "Honorarios Contables Mensuales"

    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    @ToString.Exclude // Evita bucles infinitos al imprimir logs
    @JsonIgnore
    private Invoice invoice;
}