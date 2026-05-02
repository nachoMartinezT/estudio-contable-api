package com.guidapixel.contable.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String templateType;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 1000)
    private String errorMessage;
}
