package com.guidapixel.contable.service;

import com.guidapixel.contable.domain.model.Role;
import com.guidapixel.contable.domain.model.Tenant;
import com.guidapixel.contable.domain.model.User;
import com.guidapixel.contable.domain.repository.TenantRepository;
import com.guidapixel.contable.domain.repository.UserRepository;
import com.guidapixel.contable.security.JwtService;
import com.guidapixel.contable.web.dto.AuthenticationRequest;
import com.guidapixel.contable.web.dto.AuthenticationResponse;
import com.guidapixel.contable.web.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional // Importante: si falla algo, que no guarde nada (ni tenant ni user)
    public AuthenticationResponse register(RegisterRequest request) {

        // 1. Crear y Guardar el Tenant (Estudio)
        var tenant = Tenant.builder()
                .razonSocial(request.getNombreEstudio())
                .cuit(request.getCuitEstudio())
                .emailContacto(request.getEmail())
                .build();

        var savedTenant = tenantRepository.save(tenant);

        // 2. Crear el Usuario Admin vinculado a ese Tenant
        var user = User.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();

        // Asignamos MANUALMENTE el ID del tenant, porque TenantContext está vacío ahora
        user.setTenantId(savedTenant.getId());

        userRepository.save(user);

        // 3. Generar Token
        var jwtToken = jwtService.generateToken(user, savedTenant.getId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Esto autentica usando Spring Security (verifica password)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Si llegó acá, la contraseña es correcta. Buscamos al usuario.
        // NOTA: userRepository.findByEmail busca en todos los tenants porque
        // en el login aún no tenemos contexto seteado.
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        // Generamos el token incluyendo su Tenant ID
        var jwtToken = jwtService.generateToken(user, user.getTenantId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}