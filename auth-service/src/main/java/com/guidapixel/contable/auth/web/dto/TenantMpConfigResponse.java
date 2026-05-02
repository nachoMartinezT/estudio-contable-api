package com.guidapixel.contable.auth.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMpConfigResponse {
    private Long tenantId;
    private String publicKey;
    private boolean mpEnabled;
    private boolean configured;
}
