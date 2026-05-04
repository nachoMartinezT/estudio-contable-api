package com.guidapixel.contable.invoice.domain.repository;

import com.guidapixel.contable.invoice.domain.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i")
    BigDecimal sumTotalFacturado();

    Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);

    Page<Invoice> findByTenantIdAndEstado(Long tenantId, String estado, Pageable pageable);
}
