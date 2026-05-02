package com.guidapixel.contable.auth.service;

import com.guidapixel.contable.auth.client.NotificationClient;
import com.guidapixel.contable.auth.domain.model.Module;
import com.guidapixel.contable.auth.domain.model.Role;
import com.guidapixel.contable.auth.domain.model.StaffPermissions;
import com.guidapixel.contable.auth.domain.model.Subscription;
import com.guidapixel.contable.auth.domain.model.Tenant;
import com.guidapixel.contable.auth.domain.model.User;
import com.guidapixel.contable.auth.domain.repository.StaffPermissionsRepository;
import com.guidapixel.contable.auth.domain.repository.SubscriptionRepository;
import com.guidapixel.contable.auth.domain.repository.TenantRepository;
import com.guidapixel.contable.auth.domain.repository.UserRepository;
import com.guidapixel.contable.auth.web.dto.AdminSubscriptionRequest;
import com.guidapixel.contable.auth.web.dto.CreateTenantRequest;
import com.guidapixel.contable.auth.web.dto.CreateUserRequest;
import com.guidapixel.contable.auth.web.dto.StaffPermissionsResponse;
import com.guidapixel.contable.auth.web.dto.UpdateStaffPermissionsRequest;
import com.guidapixel.contable.auth.web.dto.UpdateTenantAfipConfigRequest;
import com.guidapixel.contable.auth.web.dto.UpdateTenantMpConfigRequest;
import com.guidapixel.contable.auth.web.dto.TenantMpConfigResponse;
import com.guidapixel.contable.shared.model.TenantAfipConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StaffPermissionsRepository staffPermissionsRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesEncryptionService;
    private final NotificationClient notificationClient;

    @Value("${afip.cert.storage.path:/app/certs}")
    private String certStoragePath;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 12;

    public List<Map<String, Object>> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::tenantToMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTenantWithSubscriptions(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        List<Subscription> subscriptions = subscriptionRepository.findByTenantId(tenantId);
        List<String> activeModules = subscriptions.stream()
                .filter(Subscription::isActive)
                .map(Subscription::getModuleName)
                .collect(Collectors.toList());

        Map<String, Object> result = tenantToMap(tenant);
        result.put("subscriptions", subscriptions.stream()
                .map(this::subscriptionToMap)
                .collect(Collectors.toList()));
        result.put("activeModules", activeModules);

        return result;
    }

    @Transactional
    public Map<String, Object> updateSubscription(Long tenantId, AdminSubscriptionRequest request) {
        String moduleName = request.getModuleName();

        Optional<Subscription> existing = subscriptionRepository.findByTenantIdAndModuleName(tenantId, moduleName);

        Subscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.setActive(request.isActive());
        } else {
            subscription = Subscription.builder()
                    .tenantId(tenantId)
                    .moduleName(moduleName)
                    .active(request.isActive())
                    .build();
        }

        subscriptionRepository.save(subscription);

        List<Subscription> allSubscriptions = subscriptionRepository.findByTenantId(tenantId);
        List<String> activeModules = allSubscriptions.stream()
                .filter(Subscription::isActive)
                .map(Subscription::getModuleName)
                .collect(Collectors.toList());

        return Map.of(
                "status", "EXITO",
                "tenantId", tenantId,
                "module", moduleName,
                "active", request.isActive(),
                "activeModules", activeModules
        );
    }

    @Transactional
    public Map<String, Object> updateAllSubscriptions(Long tenantId, AdminSubscriptionRequest request) {
        List<String> modulesToActivate = request.getModules();

        for (Module module : Module.values()) {
            String key = module.getKey();
            boolean shouldActivate = modulesToActivate.contains(key);

            Optional<Subscription> existing = subscriptionRepository.findByTenantIdAndModuleName(tenantId, key);

            if (existing.isPresent()) {
                Subscription sub = existing.get();
                sub.setActive(shouldActivate);
                subscriptionRepository.save(sub);
            } else if (shouldActivate) {
                subscriptionRepository.save(Subscription.builder()
                        .tenantId(tenantId)
                        .moduleName(key)
                        .active(true)
                        .build());
            }
        }

        List<Subscription> allSubscriptions = subscriptionRepository.findByTenantId(tenantId);
        List<String> activeModules = allSubscriptions.stream()
                .filter(Subscription::isActive)
                .map(Subscription::getModuleName)
                .collect(Collectors.toList());

        return Map.of(
                "status", "EXITO",
                "tenantId", tenantId,
                "activeModules", activeModules
        );
    }

    public Map<String, Object> getDashboardStats() {
        long totalTenants = tenantRepository.count();
        long totalUsers = userRepository.count();
        List<Subscription> allSubscriptions = subscriptionRepository.findAll();
        long activeSubscriptions = allSubscriptions.stream().filter(Subscription::isActive).count();

        Map<String, Long> modulesCount = new HashMap<>();
        for (Module module : Module.values()) {
            long count = allSubscriptions.stream()
                    .filter(s -> s.getModuleName().equals(module.getKey()) && s.isActive())
                    .count();
            modulesCount.put(module.getKey(), count);
        }

        return Map.of(
                "totalTenants", totalTenants,
                "totalUsers", totalUsers,
                "activeSubscriptions", activeSubscriptions,
                "modulesCount", modulesCount
        );
    }

    @Transactional
    public Map<String, Object> createTenant(CreateTenantRequest request) {
        if (tenantRepository.findByCuit(request.getCuitEstudio()).isPresent()) {
            throw new RuntimeException("Ya existe un estudio con ese CUIT");
        }
        if (userRepository.findByEmail(request.getEmailAdmin()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }

        Tenant tenant = Tenant.builder()
                .razonSocial(request.getNombreEstudio())
                .cuit(request.getCuitEstudio())
                .emailContacto(request.getEmailAdmin())
                .build();
        Tenant savedTenant = tenantRepository.save(tenant);

        String tempPassword = generateTempPassword();

        User adminUser = User.builder()
                .nombre(request.getNombreAdmin())
                .apellido(request.getApellidoAdmin())
                .email(request.getEmailAdmin())
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.ADMIN)
                .build();
        adminUser.setTenantId(savedTenant.getId());
        userRepository.save(adminUser);

        crearSubscripcionesDefault(savedTenant.getId());

        return Map.of(
                "status", "EXITO",
                "tenantId", savedTenant.getId(),
                "razonSocial", savedTenant.getRazonSocial(),
                "cuit", savedTenant.getCuit(),
                "adminEmail", request.getEmailAdmin(),
                "adminName", request.getNombreAdmin() + " " + request.getApellidoAdmin(),
                "tempPassword", tempPassword,
                "message", "Tenant creado. El ADMIN debe cambiar su password en el primer inicio de sesion."
        );
    }

    @Transactional
    public Map<String, Object> createUser(Long tenantId, CreateUserRequest request, Long requesterTenantId) {
        if (!tenantId.equals(requesterTenantId)) {
            throw new RuntimeException("No tienes permiso para crear usuarios en otro tenant");
        }
        if (request.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("No se puede crear un usuario SUPER_ADMIN desde aqui");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("Solo puede haber un ADMIN por tenant");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }

        String tempPassword = generateTempPassword();

        User user = User.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(request.getRole())
                .clientId(request.getClientId())
                .build();
        user.setTenantId(tenantId);
        userRepository.save(user);

        if (request.getRole() == Role.STAFF) {
            StaffPermissions permissions = StaffPermissions.builder()
                    .staffUserId(user.getId())
                    .tenantId(tenantId)
                    .build();
            staffPermissionsRepository.save(permissions);
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        String tenantName = tenant != null ? tenant.getRazonSocial() : "Estudio";
        String loginUrl = appBaseUrl;

        notificationClient.sendWelcomeEmail(
                request.getEmail(),
                request.getNombre(),
                request.getApellido(),
                tenantName,
                tenantId,
                tempPassword,
                loginUrl
        );

        return Map.of(
                "status", "EXITO",
                "userId", user.getId(),
                "email", request.getEmail(),
                "role", request.getRole().name(),
                "tempPassword", tempPassword
        );
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

    @Transactional
    public Map<String, Object> updateAfipConfig(Long tenantId, UpdateTenantAfipConfigRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        if (request.getAfipCuit() != null) {
            tenant.setAfipCuit(request.getAfipCuit());
        }
        if (request.getAfipCertPassword() != null && !request.getAfipCertPassword().isBlank()) {
            tenant.setAfipCertPassword(aesEncryptionService.encrypt(request.getAfipCertPassword()));
        }
        if (request.getAfipHomologacion() != null) {
            tenant.setAfipHomologacion(request.getAfipHomologacion());
        }
        if (tenant.getAfipCertPath() == null || tenant.getAfipCertPath().isBlank()) {
            tenant.setAfipCertPath(certStoragePath + "/" + tenantId + ".p12");
        }

        tenantRepository.save(tenant);

        return Map.of(
                "status", "EXITO",
                "tenantId", tenantId,
                "afipCuit", tenant.getAfipCuit(),
                "afipCertPath", tenant.getAfipCertPath(),
                "afipHomologacion", tenant.isAfipHomologacion(),
                "message", "Configuracion AFIP actualizada"
        );
    }

    public TenantAfipConfig getTenantAfipConfig(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        if (tenant.getAfipCuit() == null || tenant.getAfipCertPassword() == null) {
            throw new RuntimeException("El tenant no tiene configuracion AFIP. Cargue el certificado y CUIT primero.");
        }

        return TenantAfipConfig.builder()
                .tenantId(tenant.getId())
                .afipCuit(tenant.getAfipCuit())
                .afipCertPassword(aesEncryptionService.decrypt(tenant.getAfipCertPassword()))
                .afipCertPath(tenant.getAfipCertPath())
                .afipHomologacion(tenant.isAfipHomologacion())
                .build();
    }

    @Transactional
    public Map<String, Object> updateMpConfig(Long tenantId, UpdateTenantMpConfigRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            tenant.setMpAccessToken(aesEncryptionService.encrypt(request.getAccessToken()));
        }
        if (request.getPublicKey() != null) {
            tenant.setMpPublicKey(request.getPublicKey());
        }
        if (request.getWebhookSecret() != null && !request.getWebhookSecret().isBlank()) {
            tenant.setMpWebhookSecret(aesEncryptionService.encrypt(request.getWebhookSecret()));
        }
        if (request.getMpEnabled() != null) {
            tenant.setMpEnabled(request.getMpEnabled());
        }

        tenantRepository.save(tenant);

        return Map.of(
                "status", "EXITO",
                "tenantId", tenantId,
                "mpEnabled", tenant.isMpEnabled(),
                "message", "Configuracion de MercadoPago actualizada"
        );
    }

    public TenantMpConfigResponse getTenantMpConfig(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        boolean configured = tenant.getMpAccessToken() != null && !tenant.getMpAccessToken().isBlank();

        return TenantMpConfigResponse.builder()
                .tenantId(tenant.getId())
                .publicKey(tenant.getMpPublicKey())
                .mpEnabled(tenant.isMpEnabled())
                .configured(configured)
                .build();
    }

    public String getDecryptedMpAccessToken(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        if (tenant.getMpAccessToken() == null || tenant.getMpAccessToken().isBlank()) {
            throw new RuntimeException("El tenant no tiene access token de MercadoPago configurado");
        }

        return aesEncryptionService.decrypt(tenant.getMpAccessToken());
    }

    public String getDecryptedMpWebhookSecret(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        if (tenant.getMpWebhookSecret() == null || tenant.getMpWebhookSecret().isBlank()) {
            throw new RuntimeException("El tenant no tiene webhook secret de MercadoPago configurado");
        }

        return aesEncryptionService.decrypt(tenant.getMpWebhookSecret());
    }

    public boolean isMpEnabled(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
        return tenant.isMpEnabled();
    }

    public boolean isOverdueReminderEnabled(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
        return tenant.isOverdueReminderEnabled();
    }

    @Transactional
    public void updateOverdueReminderEnabled(Long tenantId, boolean enabled) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
        tenant.setOverdueReminderEnabled(enabled);
        tenantRepository.save(tenant);
    }

    public Set<Long> getOverdueReminderEnabledTenantIds(Set<Long> tenantIds) {
        return tenantRepository.findAllById(tenantIds).stream()
                .filter(Tenant::isOverdueReminderEnabled)
                .map(Tenant::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Map<String, Object> getTenantName(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
        return Map.of("name", tenant.getRazonSocial(), "id", tenantId);
    }

    public Map<String, Object> getTenantContactEmail(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
        return Map.of("email", tenant.getEmailContacto() != null ? tenant.getEmailContacto() : "", "tenantId", tenantId);
    }

    @Transactional
    public Map<String, Object> uploadCert(Long tenantId, MultipartFile file) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        if (!file.getOriginalFilename().endsWith(".p12")) {
            throw new RuntimeException("Solo se aceptan archivos .p12");
        }

        try {
            Path certDir = Paths.get(certStoragePath);
            if (!Files.exists(certDir)) {
                Files.createDirectories(certDir);
            }

            Path certPath = certDir.resolve(tenantId + ".p12");
            file.transferTo(certPath.toFile());

            tenant.setAfipCertPath(certPath.toString());
            tenantRepository.save(tenant);

            return Map.of(
                    "status", "EXITO",
                    "tenantId", tenantId,
                    "certPath", certPath.toString(),
                    "message", "Certificado subido exitosamente"
            );
        } catch (IOException e) {
            throw new RuntimeException("Error al subir el certificado: " + e.getMessage());
        }
    }

    @Transactional
    public StaffPermissionsResponse updateStaffPermissions(Long tenantId, Long userId, UpdateStaffPermissionsRequest request, Long requesterTenantId) {
        if (!tenantId.equals(requesterTenantId)) {
            throw new RuntimeException("No tienes permiso para modificar permisos en otro tenant");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() != Role.STAFF) {
            throw new RuntimeException("Solo se pueden modificar permisos de usuarios STAFF");
        }
        if (!user.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El usuario no pertenece a este tenant");
        }

        StaffPermissions permissions = staffPermissionsRepository.findByStaffUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("Permisos no encontrados para este usuario"));

        permissions.setCanManageClients(request.isCanManageClients());
        permissions.setCanViewInvoices(request.isCanViewInvoices());
        permissions.setCanCreateInvoices(request.isCanCreateInvoices());
        permissions.setCanManageDocuments(request.isCanManageDocuments());
        permissions.setCanViewDashboard(request.isCanViewDashboard());
        permissions.setCanManageStaff(request.isCanManageStaff());

        staffPermissionsRepository.save(permissions);

        return StaffPermissionsResponse.builder()
                .id(permissions.getId())
                .staffUserId(permissions.getStaffUserId())
                .staffEmail(user.getEmail())
                .staffName(user.getNombre() + " " + user.getApellido())
                .canManageClients(permissions.isCanManageClients())
                .canViewInvoices(permissions.isCanViewInvoices())
                .canCreateInvoices(permissions.isCanCreateInvoices())
                .canManageDocuments(permissions.isCanManageDocuments())
                .canViewDashboard(permissions.isCanViewDashboard())
                .canManageStaff(permissions.isCanManageStaff())
                .build();
    }

    public List<String> getStaffPermissions(Long userId) {
        return staffPermissionsRepository.findByStaffUserId(userId)
                .map(permissions -> {
                    List<String> perms = new ArrayList<>();
                    if (permissions.isCanManageClients()) perms.add("MANAGE_CLIENTS");
                    if (permissions.isCanViewInvoices()) perms.add("VIEW_INVOICES");
                    if (permissions.isCanCreateInvoices()) perms.add("CREATE_INVOICES");
                    if (permissions.isCanManageDocuments()) perms.add("MANAGE_DOCUMENTS");
                    if (permissions.isCanViewDashboard()) perms.add("VIEW_DASHBOARD");
                    if (permissions.isCanManageStaff()) perms.add("MANAGE_STAFF");
                    return perms;
                })
                .orElse(Collections.emptyList());
    }

    public List<Map<String, Object>> getStaffUsers(Long tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getTenantId().equals(tenantId) && u.getRole() == Role.STAFF)
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "nombre", u.getNombre(),
                        "apellido", u.getApellido(),
                        "email", u.getEmail(),
                        "tenantId", u.getTenantId()
                ))
                .collect(Collectors.toList());
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public Map<String, Object> getAllTenantSubscriptions() {
        List<Tenant> tenants = tenantRepository.findAll();
        Map<String, List<String>> subscriptions = new HashMap<>();

        for (Tenant tenant : tenants) {
            List<String> activeModules = subscriptionRepository.findByTenantIdAndActiveTrue(tenant.getId()).stream()
                    .map(Subscription::getModuleName)
                    .collect(Collectors.toList());
            subscriptions.put(tenant.getId().toString(), activeModules);
        }

        return Map.of("subscriptions", subscriptions);
    }

    private Map<String, Object> tenantToMap(Tenant tenant) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tenant.getId());
        map.put("razonSocial", tenant.getRazonSocial());
        map.put("cuit", tenant.getCuit());
        map.put("emailContacto", tenant.getEmailContacto());
        map.put("activo", tenant.isActivo());
        map.put("createdAt", tenant.getCreatedAt());
        return map;
    }

    private Map<String, Object> subscriptionToMap(Subscription subscription) {
        return Map.of(
                "id", subscription.getId(),
                "moduleName", subscription.getModuleName(),
                "active", subscription.isActive(),
                "createdAt", subscription.getCreatedAt().toString(),
                "updatedAt", subscription.getUpdatedAt() != null ? subscription.getUpdatedAt().toString() : ""
        );
    }
}
