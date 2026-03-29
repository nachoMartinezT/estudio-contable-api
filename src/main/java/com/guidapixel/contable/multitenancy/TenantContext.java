package com.guidapixel.contable.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mantiene el ID del Tenant (Estudio Contable) para el hilo de ejecución actual.
 * Utiliza ThreadLocal para asegurar el aislamiento entre peticiones simultáneas.
 */
public class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    // ThreadLocal asegura que cada usuario/hilo tenga su propia variable aislada
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        log.debug("Estableciendo contexto para Tenant ID: {}", tenantId);
        currentTenant.set(tenantId);
    }

    public static Long getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        log.debug("Limpiando contexto del Tenant");
        currentTenant.remove();
    }
}