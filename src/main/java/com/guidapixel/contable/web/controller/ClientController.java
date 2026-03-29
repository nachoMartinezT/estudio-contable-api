package com.guidapixel.contable.web.controller;

import com.guidapixel.contable.domain.model.Client;
import com.guidapixel.contable.domain.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        // OJO: No seteamos el TenantId manualmente.
        // El @PrePersist de BaseEntity lo hará solo leyendo el contexto.
        return ResponseEntity.ok(clientRepository.save(client));
    }

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        // OJO: El Aspecto de Hibernate filtrará esto automáticamente.
        return ResponseEntity.ok(clientRepository.findAll());
    }
}