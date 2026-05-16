package com.guidapixel.contable.auth.web.dto;

import lombok.Data;

@Data
public class UpdateMyTenantRequest {
    private String razonSocial;
    private String cuit;
    private String emailContacto;
}
