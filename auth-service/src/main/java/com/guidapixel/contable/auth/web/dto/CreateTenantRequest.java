package com.guidapixel.contable.auth.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {
    private String nombreEstudio;
    private String cuitEstudio;
    private String nombreAdmin;
    private String apellidoAdmin;
    private String emailAdmin;
}
