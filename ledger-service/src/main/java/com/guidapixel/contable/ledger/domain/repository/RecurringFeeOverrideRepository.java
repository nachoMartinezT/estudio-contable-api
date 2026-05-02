package com.guidapixel.contable.ledger.domain.repository;

import com.guidapixel.contable.ledger.domain.model.RecurringFeeOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringFeeOverrideRepository extends JpaRepository<RecurringFeeOverride, Long> {

    Optional<RecurringFeeOverride> findByRecurringFeeIdAndYearMonth(Long recurringFeeId, String yearMonth);

    Optional<RecurringFeeOverride> findByTenantIdAndClientIdAndYearMonth(Long tenantId, Long clientId, String yearMonth);

    List<RecurringFeeOverride> findByTenantIdAndYearMonth(Long tenantId, String yearMonth);
}
