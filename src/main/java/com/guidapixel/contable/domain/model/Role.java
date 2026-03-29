package com.guidapixel.contable.domain.model;

public enum Role {
    ADMIN,  // Dueño del estudio (puede ver facturación del SaaS, crear usuarios)
    STAFF,  // Contador empleado (carga facturas, ve clientes)
    CLIENT  // Cliente final (solo ve sus propios impuestos, futuro portal)
}