package com.guidapixel.contable.domain.repository;

import com.guidapixel.contable.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Gracias a BaseEntity, esto ya filtra por Tenant

    // JPQL Query: Suma el campo 'total' de todas las facturas.
    // El Aspecto de Hibernate inyectará el filtro "WHERE tenant_id = X" automáticamente.
    // Usamos COALESCE para que si no hay facturas devuelva 0 en vez de null.
    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i")
    BigDecimal sumTotalFacturado();
}