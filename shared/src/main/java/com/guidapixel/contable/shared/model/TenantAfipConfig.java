package com.guidapixel.contable.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAfipConfig {
    private Long tenantId;
    private String afipCuit;
    private String afipCertPassword;
    private String afipCertPath;
    private boolean afipHomologacion;
}
