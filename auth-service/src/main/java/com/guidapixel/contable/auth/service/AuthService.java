package com.guidapixel.contable.auth.service;

import com.guidapixel.contable.auth.client.NotificationClient;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AdminService adminService;
    private final AuthenticationManager authenticationManager;
    private final NotificationClient notificationClient;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

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
                Module.DASHBOARD,
                Module.DOCUMENTS
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

        List<String> permissions = null;
        if (user.getRole() == Role.STAFF) {
            permissions = adminService.getStaffPermissions(user.getId());
        }

        var jwtToken = jwtService.generateToken(user, user.getTenantId(), permissions, user.getClientId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public com.guidapixel.contable.auth.web.dto.UserProfileResponse getUserProfile(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        var tenant = tenantRepository.findById(user.getTenantId())
                .orElse(null);

        return com.guidapixel.contable.auth.web.dto.UserProfileResponse.builder()
                .id(user.getId())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .email(user.getEmail())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .tenantName(tenant != null ? tenant.getRazonSocial() : null)
                .clientId(user.getClientId())
                .build();
    }

    @Transactional
    public Map<String, Object> changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Map.of("status", "EXITO", "mensaje", "Contraseña actualizada correctamente");
    }

    @Transactional
    public Map<String, Object> forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No existe un usuario con ese email"));

        String token = generateResetToken();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        String resetUrl = appBaseUrl + "/reset-password?token=" + token;
        String tenantName = null;
        Long tenantId = user.getTenantId();
        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            tenantName = tenant != null ? tenant.getRazonSocial() : null;
        }

        notificationClient.sendPasswordResetEmail(
                user.getEmail(),
                user.getNombre() + " " + user.getApellido(),
                resetUrl,
                tenantId,
                tenantName
        );

        return Map.of("status", "EXITO", "mensaje", "Se ha enviado un email con instrucciones para recuperar tu contraseña");
    }

    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalido o expirado"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El token ha expirado. Solicita uno nuevo.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return Map.of("status", "EXITO", "mensaje", "Contraseña restablecida correctamente");
    }

    @Transactional
    public Map<String, Object> adminResetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return Map.of(
                "status", "EXITO",
                "mensaje", "Contraseña regenerada correctamente",
                "tempPassword", tempPassword
        );
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
