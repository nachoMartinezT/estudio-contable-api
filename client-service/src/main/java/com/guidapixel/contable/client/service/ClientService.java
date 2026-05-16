package com.guidapixel.contable.client.service;

import com.guidapixel.contable.client.domain.model.Client;
import com.guidapixel.contable.client.domain.repository.ClientRepository;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-service.url:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${services.ledger-service.url:http://ledger-service:8088}")
    private String ledgerServiceUrl;

    @Value("${internal-api-key:${internal.api.key:}}")
    private String internalApiKey;

    @Transactional(readOnly = true)
    public List<Client> getActiveClients() {
        return clientRepository.findByTenantIdAndActivoTrueOrderByRazonSocialAsc(requireTenantId());
    }

    @Transactional(readOnly = true)
    public long countActiveClients() {
        return clientRepository.countByTenantIdAndActivoTrue(requireTenantId());
    }

    @Transactional(readOnly = true)
    public Client getClient(Long id) {
        Long tenantId = requireTenantId();
        return clientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    @Transactional
    public Client createClient(Client client) {
        Long tenantId = requireTenantId();
        client.setTenantId(tenantId);
        client.setCuit(normalizeCuit(client.getCuit()));
        validateClient(client);

        if (clientRepository.existsByTenantIdAndCuitAndActivoTrue(tenantId, client.getCuit())) {
            throw new RuntimeException("Ya existe un cliente activo con ese CUIT para este tenant");
        }

        Client saved = clientRepository.save(client);
        createClientUserIfNeeded(saved);
        syncRecurringFee(saved);
        return saved;
    }

    @Transactional
    public Client updateClient(Long id, Client clientData) {
        Long tenantId = requireTenantId();
        Client existing = clientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        existing.setRazonSocial(clientData.getRazonSocial());
        existing.setCuit(normalizeCuit(clientData.getCuit()));
        existing.setEmail(clientData.getEmail());
        existing.setTelefono(clientData.getTelefono());
        existing.setCondicionIVA(clientData.getCondicionIVA());
        existing.setHonorarioMensual(clientData.getHonorarioMensual());
        validateClient(existing);
        clientRepository.findByTenantIdAndCuitAndActivoTrue(tenantId, existing.getCuit())
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> {
                    throw new RuntimeException("Ya existe otro cliente activo con ese CUIT para este tenant");
                });

        Client saved = clientRepository.save(existing);
        createClientUserIfNeeded(saved);
        syncRecurringFee(saved);
        return saved;
    }

    @Transactional
    public void deactivateClient(Long id) {
        Long tenantId = requireTenantId();
        Client client = clientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        client.setActivo(false);
        clientRepository.save(client);
        syncRecurringFee(client);
    }

    @Transactional
    public Client reactivateClient(Long id) {
        Long tenantId = requireTenantId();
        Client client = clientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        client.setActivo(true);
        Client saved = clientRepository.save(client);
        syncRecurringFee(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Client> getInactiveClients() {
        return clientRepository.findByTenantIdAndActivoFalseOrderByRazonSocialAsc(requireTenantId());
    }

    private void createClientUserIfNeeded(Client client) {
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            return;
        }
        try {
            createClientUser(client);
        } catch (Exception e) {
            log.warn("No se pudo crear/actualizar el usuario CLIENT para el cliente {}: {}", client.getId(), e.getMessage());
        }
    }

    private void createClientUser(Client client) {
        String url = authServiceUrl + "/api/internal/tenants/" + client.getTenantId() + "/client-users";

        Map<String, Object> body = Map.of(
                "email", client.getEmail(),
                "nombre", client.getRazonSocial(),
                "apellido", "",
                "clientId", client.getId()
        );

        restTemplate.postForEntity(url, new HttpEntity<>(body, internalHeaders()), Map.class);
        log.info("Usuario CLIENT creado/actualizado para cliente {} con email {}", client.getId(), client.getEmail());
    }

    private void syncRecurringFee(Client client) {
        try {
            String url = ledgerServiceUrl + "/api/internal/ledger/recurring-fees/sync";
            BigDecimal amount = client.getHonorarioMensual() != null ? client.getHonorarioMensual() : BigDecimal.ZERO;
            Map<String, Object> body = Map.of(
                    "tenantId", client.getTenantId(),
                    "clientId", client.getId(),
                    "clientEmail", client.getEmail() != null ? client.getEmail() : "",
                    "clientName", client.getRazonSocial(),
                    "baseAmount", amount,
                    "active", client.isActivo() && amount.compareTo(BigDecimal.ZERO) > 0
            );
            restTemplate.postForEntity(url, new HttpEntity<>(body, internalHeaders()), Map.class);
        } catch (Exception e) {
            log.warn("No se pudo sincronizar honorario recurrente para cliente {}: {}", client.getId(), e.getMessage());
        }
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }
        return headers;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }
        return tenantId;
    }

    private void validateClient(Client client) {
        if (client.getRazonSocial() == null || client.getRazonSocial().isBlank()) {
            throw new IllegalArgumentException("La razon social es obligatoria");
        }
        if (client.getCuit() == null || client.getCuit().length() != 11) {
            throw new IllegalArgumentException("El CUIT debe tener 11 digitos numericos");
        }
        if (client.getHonorarioMensual() != null && client.getHonorarioMensual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El honorario mensual no puede ser negativo");
        }
    }

    private String normalizeCuit(String cuit) {
        return cuit == null ? null : cuit.replaceAll("\\D", "");
    }
}
