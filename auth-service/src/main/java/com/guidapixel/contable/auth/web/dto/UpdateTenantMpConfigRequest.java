package com.guidapixel.contable.auth.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantMpConfigRequest {
    private String accessToken;
    private String publicKey;
    private String webhookSecret;
    private Boolean mpEnabled;
}
