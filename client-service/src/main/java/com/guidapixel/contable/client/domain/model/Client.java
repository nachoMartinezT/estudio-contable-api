package com.guidapixel.contable.client.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Client extends BaseEntity {

    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false)
    private String cuit;

    private String email;
    private String telefono;

    @Column(name = "condicion_iva")
    private String condicionIVA;

    @Column(name = "honorario_mensual")
    private java.math.BigDecimal honorarioMensual;

    @Builder.Default
    private boolean activo = true;

    public String getEstado() {
        return activo ? "ACTIVO" : "INACTIVO";
    }
}
