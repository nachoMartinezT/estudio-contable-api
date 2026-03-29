package com.guidapixel.contable.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantFilterAspect {

    @PersistenceContext
    private final EntityManager entityManager;

    // Esta expresión mágica dice: "Ejecútate antes de cualquier método de cualquier clase dentro de 'repository'"
    @Before("execution(* com.guidapixel.contable.domain.repository..*.*(..))")
    public void enableTenantFilter() {
        // 1. Obtenemos el ID del tenant actual desde el contexto (que llenó el JwtFilter)
        Long tenantId = TenantContext.getTenantId();

        // 2. Si hay un tenant identificado, activamos el filtro de Hibernate
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);

            // "tenantFilter" es el nombre que le pusimos en @FilterDef en BaseEntity
            session.enableFilter("tenantFilter")
                    .setParameter("tenantId", tenantId);

            System.out.println("🛡️ Filtro activado para Tenant ID: " + tenantId);
        } else {
            System.out.println("⚠️ OJO: Consulta sin Tenant ID (Probablemente login o registro)");
        }
    }
}