package com.guidapixel.contable.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class RegisterRequest {
    // Datos del Estudio (Tenant)
    private String nombreEstudio;
    private String cuitEstudio;

    // Datos del Usuario Administrador
    private String nombre;
    private String apellido;
    private String email;
    private String password;
}