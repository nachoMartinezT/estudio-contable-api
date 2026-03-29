package com.guidapixel.contable.domain.repository;

import com.guidapixel.contable.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCuit(String cuit);
}