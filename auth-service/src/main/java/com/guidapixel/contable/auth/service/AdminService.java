package com.guidapixel.contable.auth.service;

import com.guidapixel.contable.auth.domain.model.Module;
import com.guidapixel.contable.auth.domain.model.Subscription;
import com.guidapixel.contable.auth.domain.model.Tenant;
import com.guidapixel.contable.auth.domain.model.User;
import com.guidapixel.contable.auth.domain.repository.SubscriptionRepository;
import com.guidapixel.contable.auth.domain.repository.TenantRepository;
import com.guidapixel.contable.auth.domain.repository.UserRepository;
import com.guidapixel.contable.auth.web.dto.AdminSubscriptionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

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
