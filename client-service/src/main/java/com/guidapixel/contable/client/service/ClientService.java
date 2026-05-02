package com.guidapixel.contable.client.service;

import com.guidapixel.contable.client.domain.model.Client;
import com.guidapixel.contable.client.domain.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-service.url:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${internal-api-key:}")
    private String internalApiKey;

    @Transactional
    public Client createClient(Client client) {
        Client saved = clientRepository.save(client);

        if (client.getEmail() != null && !client.getEmail().isBlank()) {
            try {
                createClientUser(saved);
            } catch (Exception e) {
                log.warn("No se pudo crear el usuario CLIENT para el cliente {}: {}", saved.getId(), e.getMessage());
            }
        }

        return saved;
    }

    private void createClientUser(Client client) {
        String url = authServiceUrl + "/api/internal/tenants/" + client.getTenantId() + "/client-users";

        Map<String, Object> body = Map.of(
                "email", client.getEmail(),
                "nombre", client.getRazonSocial(),
                "apellido", "",
                "clientId", client.getId()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Key", internalApiKey);
        }

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        log.info("Usuario CLIENT creado para cliente {} con email {}", client.getId(), client.getEmail());
    }
}
