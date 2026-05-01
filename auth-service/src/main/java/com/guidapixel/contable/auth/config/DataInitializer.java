package com.guidapixel.contable.auth.config;

import com.guidapixel.contable.auth.domain.model.Role;
import com.guidapixel.contable.auth.domain.model.Tenant;
import com.guidapixel.contable.auth.domain.model.User;
import com.guidapixel.contable.auth.domain.repository.TenantRepository;
import com.guidapixel.contable.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("No hay usuarios. Creando super-admin por defecto...");

            Tenant ownerTenant = Tenant.builder()
                    .razonSocial("Guida Pixel SaaS")
                    .cuit("30-00000000-0")
                    .emailContacto("admin@guidapixel.com")
                    .build();
            tenantRepository.save(ownerTenant);

            User superAdmin = User.builder()
                    .nombre("Admin")
                    .apellido("Guida Pixel")
                    .email("admin@guidapixel.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.SUPER_ADMIN)
                    .build();
            superAdmin.setTenantId(ownerTenant.getId());
            userRepository.save(superAdmin);

            log.info("Super-admin creado: admin@guidapixel.com / admin123");
        }
    }
}
