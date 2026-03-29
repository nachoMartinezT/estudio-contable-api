package com.guidapixel.contable.domain.repository;

import com.guidapixel.contable.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Gracias al Aspecto y BaseEntity, este método buscará:
    // WHERE email = ? AND tenant_id = (contexto actual)
    Optional<User> findByEmail(String email);

    // Método especial para el Login (aquí NO queremos filtrar por tenant todavía,
    // porque el usuario recién está llegando y no sabemos de qué tenant es)
    // Usamos @Query nativa o desactivamos el filtro manualmente en el servicio de Auth.
    // Por ahora, lo dejamos estándar.
}