package com.guidapixel.contable.invoice.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseEntity {

    @Column(nullable = false)
    private String numeroFactura;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    private BigDecimal total;

    // Campos AFIP
    private String cae;
    private LocalDate vencimientoCae;
    private Integer tipoComprobante;
    private Integer puntoVenta;
    private Integer nroComprobanteAfip;
    private String estadoAfip;

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public boolean isEmitidaAfip() {
        return cae != null && !cae.isBlank();
    }
}
