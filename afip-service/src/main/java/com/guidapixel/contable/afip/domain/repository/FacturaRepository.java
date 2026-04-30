package com.guidapixel.contable.afip.domain.repository;

import com.guidapixel.contable.afip.domain.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
}
