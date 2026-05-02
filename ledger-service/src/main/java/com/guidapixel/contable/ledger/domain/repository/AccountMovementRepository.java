package com.guidapixel.contable.ledger.domain.repository;

import com.guidapixel.contable.ledger.domain.model.AccountMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountMovementRepository extends JpaRepository<AccountMovement, Long> {

    Page<AccountMovement> findByTenantIdAndClientIdOrderByCreatedAtDesc(Long tenantId, Long clientId, Pageable pageable);

    List<AccountMovement> findByTenantIdAndClientIdAndPaidAtIsNull(Long tenantId, Long clientId);

    List<AccountMovement> findByTenantIdAndClientIdAndPaidAtIsNotNull(Long tenantId, Long clientId);

    java.util.Optional<AccountMovement> findByMpPreferenceId(String mpPreferenceId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT m FROM AccountMovement m WHERE m.paidAt IS NULL AND m.dueDate IS NOT NULL AND m.dueDate BETWEEN :start AND :end"
    )
    List<AccountMovement> findUpcomingDue(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDate end
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM AccountMovement m
        WHERE m.paidAt IS NULL
        AND m.dueDate IS NOT NULL
        AND m.dueDate < :today
        AND m.direction = 'DEBIT'
        AND NOT EXISTS (
            SELECT r FROM OverdueReminderLog r
            WHERE r.movementId = m.id
            AND r.sentAt > :sevenDaysAgo
        )
    """)
    List<AccountMovement> findOverdueNotRecentlyReminded(
            @org.springframework.data.repository.query.Param("today") java.time.LocalDate today,
            @org.springframework.data.repository.query.Param("sevenDaysAgo") java.time.LocalDateTime sevenDaysAgo
    );

    java.util.Optional<AccountMovement> findByTenantIdAndClientIdAndDescriptionAndCreatedAtBetween(
            Long tenantId, Long clientId, String description, java.time.LocalDateTime start, java.time.LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query(
            "SELECT m FROM AccountMovement m WHERE m.tenantId = :tenantId AND m.clientId = :clientId AND m.createdAt >= :from AND m.createdAt <= :to ORDER BY m.createdAt ASC"
    )
    List<AccountMovement> findByTenantAndClientAndDateRange(
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            @org.springframework.data.repository.query.Param("clientId") Long clientId,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to
    );

    @org.springframework.data.jpa.repository.Query(
            "SELECT m FROM AccountMovement m WHERE m.tenantId = :tenantId AND m.createdAt >= :from AND m.createdAt <= :to ORDER BY m.createdAt ASC"
    )
    List<AccountMovement> findByTenantAndDateRange(
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to
    );
}
