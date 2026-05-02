package com.guidapixel.contable.auth.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantAfipConfigRequest {
    private String afipCuit;
    private String afipCertPassword;
    private Boolean afipHomologacion;
}
