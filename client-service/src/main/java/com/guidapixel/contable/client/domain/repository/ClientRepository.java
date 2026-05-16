package com.guidapixel.contable.client.domain.repository;

import com.guidapixel.contable.client.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByTenantIdAndActivoTrueOrderByRazonSocialAsc(Long tenantId);
    List<Client> findByTenantIdAndActivoFalseOrderByRazonSocialAsc(Long tenantId);
    long countByTenantIdAndActivoTrue(Long tenantId);
    Optional<Client> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Client> findByTenantIdAndCuitAndActivoTrue(Long tenantId, String cuit);
    boolean existsByTenantIdAndCuitAndActivoTrue(Long tenantId, String cuit);
}
