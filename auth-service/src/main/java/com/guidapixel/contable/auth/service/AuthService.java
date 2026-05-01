package com.guidapixel.contable.auth.service;

import com.guidapixel.contable.auth.domain.model.Module;
import com.guidapixel.contable.auth.domain.model.Role;
import com.guidapixel.contable.auth.domain.model.Subscription;
import com.guidapixel.contable.auth.domain.model.Tenant;
import com.guidapixel.contable.auth.domain.model.User;
import com.guidapixel.contable.auth.domain.repository.SubscriptionRepository;
import com.guidapixel.contable.auth.domain.repository.TenantRepository;
import com.guidapixel.contable.auth.domain.repository.UserRepository;
import com.guidapixel.contable.auth.web.dto.AuthenticationRequest;
import com.guidapixel.contable.auth.web.dto.AuthenticationResponse;
import com.guidapixel.contable.auth.web.dto.RegisterRequest;
import com.guidapixel.contable.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        var tenant = Tenant.builder()
                .razonSocial(request.getNombreEstudio())
                .cuit(request.getCuitEstudio())
                .emailContacto(request.getEmail())
                .build();

        var savedTenant = tenantRepository.save(tenant);

        var user = User.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();

        user.setTenantId(savedTenant.getId());
        userRepository.save(user);

        crearSubscripcionesDefault(savedTenant.getId());

        var jwtToken = jwtService.generateToken(user, savedTenant.getId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    private void crearSubscripcionesDefault(Long tenantId) {
        List<Module> modulosDefault = Arrays.asList(
                Module.CLIENTS,
                Module.INVOICES,
                Module.AFIP,
                Module.AUDIT,
                Module.DASHBOARD
        );

        for (Module modulo : modulosDefault) {
            subscriptionRepository.save(Subscription.builder()
                    .tenantId(tenantId)
                    .moduleName(modulo.getKey())
                    .active(true)
                    .build());
        }
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user, user.getTenantId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
