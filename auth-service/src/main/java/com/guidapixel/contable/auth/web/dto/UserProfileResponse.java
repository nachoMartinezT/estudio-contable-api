package com.guidapixel.contable.auth.web.dto;

import com.guidapixel.contable.auth.domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private Role role;
    private Long tenantId;
    private String tenantName;
}
