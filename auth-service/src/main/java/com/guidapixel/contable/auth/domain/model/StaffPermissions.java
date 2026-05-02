package com.guidapixel.contable.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_permissions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StaffPermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long staffUserId;

    @Column(nullable = false)
    private Long tenantId;

    @Builder.Default
    private boolean canManageClients = true;

    @Builder.Default
    private boolean canViewInvoices = false;

    @Builder.Default
    private boolean canCreateInvoices = false;

    @Builder.Default
    private boolean canManageDocuments = true;

    @Builder.Default
    private boolean canViewDashboard = false;

    @Builder.Default
    private boolean canManageStaff = false;
}
