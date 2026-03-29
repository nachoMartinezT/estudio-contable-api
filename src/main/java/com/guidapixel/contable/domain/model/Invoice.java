package com.guidapixel.contable.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseEntity {

    @Column(nullable = false)
    private String numeroFactura; // Ej: "0001-00000045"

    @Column(nullable = false)
    private LocalDate fechaEmision;

    // Relación con el Cliente (Un cliente tiene muchas facturas)
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Relación con los Ítems (Una factura tiene muchos ítems)
    // orphanRemoval = true: Si borras un ítem de la lista, se borra de la base de datos
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Para que el builder no deje la lista en null
    private List<InvoiceItem> items = new ArrayList<>();

    private BigDecimal total;

    // Método helper para agregar ítems y mantener la coherencia bidireccional
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }
}