package com.guidapixel.contable.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "overdue_reminder_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Long movementId;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
