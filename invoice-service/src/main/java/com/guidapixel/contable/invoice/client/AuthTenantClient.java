package com.guidapixel.contable.invoice.client;

import com.guidapixel.contable.shared.model.TenantAfipConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AuthTenantClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public AuthTenantClient(
            @Value("${services.auth-service.url:http://auth-service:8081}") String authUrl,
            @Value("${internal.api.key:defaultInternalKey}") String internalApiKey
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(authUrl)
                .build();
        this.internalApiKey = internalApiKey;
    }

    @SuppressWarnings("unchecked")
    public TenantAfipConfig getTenantAfipConfig(Long tenantId) {
        try {
            var response = webClient.get()
                    .uri("/api/internal/tenants/{id}/afip-config", tenantId)
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();

            if (response != null && "ERROR".equals(response.get("status"))) {
                throw new RuntimeException("Error obteniendo config AFIP del tenant: " + response.get("error"));
            }

            return TenantAfipConfig.builder()
                    .tenantId(tenantId)
                    .afipCuit((String) response.get("afipCuit"))
                    .afipCertPassword((String) response.get("afipCertPassword"))
                    .afipCertPath((String) response.get("afipCertPath"))
                    .afipHomologacion(Boolean.TRUE.equals(response.get("afipHomologacion")))
                    .build();
        } catch (Exception e) {
            log.error("Error obteniendo config AFIP del tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("Error obteniendo configuracion AFIP del tenant: " + e.getMessage(), e);
        }
    }
}
