package com.guidapixel.contable.auth.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPermissionsResponse {
    private Long id;
    private Long staffUserId;
    private String staffEmail;
    private String staffName;
    private boolean canManageClients;
    private boolean canViewInvoices;
    private boolean canCreateInvoices;
    private boolean canManageDocuments;
    private boolean canViewDashboard;
    private boolean canManageStaff;
}
