package com.guidapixel.contable.auth.domain.repository;

import com.guidapixel.contable.auth.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByTenantId(Long tenantId);

    Optional<Subscription> findByTenantIdAndModuleName(Long tenantId, String moduleName);

    List<Subscription> findByTenantIdAndActiveTrue(Long tenantId);

    boolean existsByTenantIdAndModuleNameAndActiveTrue(Long tenantId, String moduleName);
}
