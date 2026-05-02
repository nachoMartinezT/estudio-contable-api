package com.guidapixel.contable.ledger.domain.repository;

import com.guidapixel.contable.ledger.domain.model.FeeGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeGenerationLogRepository extends JpaRepository<FeeGenerationLog, Long> {

    Optional<FeeGenerationLog> findByTenantIdAndYearMonthAndSuccessTrue(Long tenantId, String yearMonth);

    List<FeeGenerationLog> findByTenantIdAndYearMonth(Long tenantId, String yearMonth);

    List<FeeGenerationLog> findByTenantIdAndClientId(Long tenantId, Long clientId);

    List<FeeGenerationLog> findByTenantIdAndClientIdAndYearMonth(Long tenantId, Long clientId, String yearMonth);
}
