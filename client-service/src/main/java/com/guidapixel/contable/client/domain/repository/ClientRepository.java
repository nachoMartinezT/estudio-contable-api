package com.guidapixel.contable.client.domain.repository;

import com.guidapixel.contable.client.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
