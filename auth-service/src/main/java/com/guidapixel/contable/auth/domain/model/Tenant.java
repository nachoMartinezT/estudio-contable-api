package com.guidapixel.contable.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false, unique = true)
    private String cuit;

    private String emailContacto;

    @Builder.Default
    private boolean activo = true;

    private LocalDateTime createdAt;

    @Column(name = "afip_cuit")
    private String afipCuit;

    @Column(name = "afip_cert_password")
    private String afipCertPassword;

    @Column(name = "afip_cert_path")
    private String afipCertPath;

    @Builder.Default
    @Column(name = "afip_homologacion")
    private boolean afipHomologacion = true;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Column(name = "mp_access_token")
    private String mpAccessToken;

    @Column(name = "mp_public_key")
    private String mpPublicKey;

    @Column(name = "mp_webhook_secret")
    private String mpWebhookSecret;

    @Builder.Default
    @Column(name = "mp_enabled")
    private boolean mpEnabled = false;

    @Builder.Default
    @Column(name = "overdue_reminder_enabled")
    private boolean overdueReminderEnabled = true;
}
