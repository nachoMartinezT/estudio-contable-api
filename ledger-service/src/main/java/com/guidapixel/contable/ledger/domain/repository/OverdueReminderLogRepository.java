package com.guidapixel.contable.ledger.domain.repository;

import com.guidapixel.contable.ledger.domain.model.OverdueReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OverdueReminderLogRepository extends JpaRepository<OverdueReminderLog, Long> {

    @Query("SELECT o.movementId FROM OverdueReminderLog o WHERE o.movementId IN :movementIds AND o.sentAt > :since")
    List<Long> findRecentlyRemindedMovementIds(
            @Param("movementIds") List<Long> movementIds,
            @Param("since") LocalDateTime since
    );
}
