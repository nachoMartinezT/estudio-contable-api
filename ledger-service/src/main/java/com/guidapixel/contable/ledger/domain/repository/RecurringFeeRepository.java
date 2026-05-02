package com.guidapixel.contable.ledger.domain.repository;

import com.guidapixel.contable.ledger.domain.model.RecurringFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringFeeRepository extends JpaRepository<RecurringFee, Long> {

    Optional<RecurringFee> findByTenantIdAndClientId(Long tenantId, Long clientId);

    List<RecurringFee> findByTenantIdAndActiveTrue(Long tenantId);
}
