package com.guidapixel.contable.auth.domain.repository;

import com.guidapixel.contable.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndActivoTrue(String email);
    List<User> findByTenantId(Long tenantId);
    List<User> findByTenantIdAndActivoTrue(Long tenantId);
    Optional<User> findByResetToken(String resetToken);
    long countByActivoTrue();
}
