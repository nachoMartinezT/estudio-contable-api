package com.guidapixel.contable.dashboard.service;

import com.guidapixel.contable.dashboard.web.dto.DashboardResponse;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.client-service.url}")
    private String clientServiceUrl;

    @Value("${services.invoice-service.url}")
    private String invoiceServiceUrl;

    @Value("${services.audit-service.url}")
    private String auditServiceUrl;

    public DashboardResponse getDashboardData(String authHeader) {
        Long tenantId = TenantContext.getTenantId();

        WebClient client = webClientBuilder.build();

        long totalClientes = client.get()
                .uri(clientServiceUrl + "/api/v1/clients/count")
                .header("Authorization", authHeader)
                .header("X-Tenant-Id", String.valueOf(tenantId))
                .retrieve()
                .bodyToMono(Long.class)
                .blockOptional()
                .orElse(0L);

        BigDecimal totalFacturado = client.get()
                .uri(invoiceServiceUrl + "/api/v1/invoices/total-facturado")
                .header("Authorization", authHeader)
                .header("X-Tenant-Id", String.valueOf(tenantId))
                .retrieve()
                .bodyToMono(BigDecimal.class)
                .blockOptional()
                .orElse(BigDecimal.ZERO);

        List ultimosMovimientos = client.get()
                .uri(auditServiceUrl + "/api/v1/audit/latest?tenantId=" + tenantId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .blockOptional()
                .orElse(List.of());

        return DashboardResponse.builder()
                .cantidadClientes(totalClientes)
                .totalFacturado(totalFacturado)
                .ultimosMovimientos(ultimosMovimientos)
                .build();
    }
}
