package com.guidapixel.contable.ledger.domain.repository;

import com.guidapixel.contable.ledger.domain.model.ClientBalance;
import com.guidapixel.contable.ledger.domain.model.ClientBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientBalanceRepository extends JpaRepository<ClientBalance, ClientBalanceId> {

    Optional<ClientBalance> findByTenantIdAndClientId(Long tenantId, Long clientId);

    List<ClientBalance> findByTenantId(Long tenantId);
}
