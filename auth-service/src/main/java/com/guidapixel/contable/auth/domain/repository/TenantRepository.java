package com.guidapixel.contable.auth.domain.repository;

import com.guidapixel.contable.auth.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCuit(String cuit);
}
