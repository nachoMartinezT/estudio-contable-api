package com.guidapixel.contable.domain.repository;

import com.guidapixel.contable.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    // Aquí puedes agregar métodos extra si necesitas, ej:
    // Optional<Client> findByCuit(String cuit);
}