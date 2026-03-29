package com.guidapixel.contable.domain.repository;

import com.guidapixel.contable.domain.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    // ¡Listo! No hay que escribir SQL. Spring hace todo.
}