package com.guidapixel.contable.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Client extends BaseEntity {

    @Column(nullable = false)
    private String razonSocial; // Ej: "Panadería Los Dos Hermanos"

    @Column(nullable = false)
    private String cuit; // CUIT del cliente del estudio

    private String email;
    private String telefono;

    // Aquí irían más datos: condición fiscal, dirección, etc.
}