package com.guidapixel.contable.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String razonSocial; // Ej: "Estudio Contable Güida"

    @Column(nullable = false, unique = true)
    private String cuit; // CUIT del estudio (para facturarles el SaaS a ellos)

    private String emailContacto; // Email administrativo

    @Builder.Default
    private boolean activo = true; // Si no pagan, lo ponemos en false

    // Configuración regional (Moneda, Timezone) podría ir acá en el futuro

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}