package com.guidapixel.contable.auth.domain.repository;

import com.guidapixel.contable.auth.domain.model.StaffPermissions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffPermissionsRepository extends JpaRepository<StaffPermissions, Long> {
    Optional<StaffPermissions> findByStaffUserId(Long staffUserId);
    Optional<StaffPermissions> findByStaffUserIdAndTenantId(Long staffUserId, Long tenantId);
}
