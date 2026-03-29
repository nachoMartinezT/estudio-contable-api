package com.guidapixel.contable.service;

import com.guidapixel.contable.domain.repository.AuditLogRepository;
import com.guidapixel.contable.domain.repository.ClientRepository;
import com.guidapixel.contable.domain.repository.InvoiceRepository;
import com.guidapixel.contable.multitenancy.TenantContext;
import com.guidapixel.contable.web.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true) // Optimizamos para lectura
    public DashboardResponse getDashboardData() {

        // 1. Datos de Postgres (SQL)
        // El count() respeta el filtro del Aspecto
        long totalClientes = clientRepository.count();
        // La suma respeta el filtro del Aspecto
        var totalFacturado = invoiceRepository.sumTotalFacturado();

        // 2. Datos de Mongo (NoSQL)
        Long tenantId = TenantContext.getTenantId();
        var ultimosLogs = auditLogRepository.findTop5ByTenantIdOrderByTimestampDesc(tenantId);

        // 3. Ensamblar respuesta
        return DashboardResponse.builder()
                .cantidadClientes(totalClientes)
                .totalFacturado(totalFacturado)
                .ultimosMovimientos(ultimosLogs)
                .build();
    }
}