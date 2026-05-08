package com.guidapixel.contable.client.web;

import com.guidapixel.contable.client.domain.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/clients")
@RequiredArgsConstructor
public class InternalClientController {

    private final ClientRepository clientRepository;

    @GetMapping("/{tenantId}/{clientId}")
    public ResponseEntity<?> getClientForTenant(@PathVariable Long tenantId, @PathVariable Long clientId) {
        return clientRepository.findByIdAndTenantId(clientId, tenantId)
                .<ResponseEntity<?>>map(client -> ResponseEntity.ok(Map.of(
                        "id", client.getId(),
                        "tenantId", client.getTenantId(),
                        "razonSocial", client.getRazonSocial(),
                        "email", client.getEmail() != null ? client.getEmail() : "",
                        "cuit", client.getCuit(),
                        "honorarioMensual", client.getHonorarioMensual() != null ? client.getHonorarioMensual() : java.math.BigDecimal.ZERO,
                        "activo", client.isActivo()
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("status", "ERROR", "error", "Cliente no encontrado")));
    }
}
