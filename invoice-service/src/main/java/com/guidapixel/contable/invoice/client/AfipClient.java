package com.guidapixel.contable.invoice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class AfipClient {

    private final WebClient webClient;

    public AfipClient(@Value("${services.afip-service.url:http://afip-service:8084}") String afipUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(afipUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> emitirFactura(AfipFacturaRequest request, Long tenantId) {
        try {
            FacturaEmitirPayload payload = FacturaEmitirPayload.builder()
                    .tenantId(tenantId)
                    .factura(request)
                    .build();

            Map<String, Object> response = webClient.post()
                    .uri("/api/afip/emitir")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "ERROR".equals(response.get("status"))) {
                throw new RuntimeException("AFIP rechazo la factura: " + response.get("mensaje"));
            }

            return (Map<String, Object>) response.get("datos_factura");
        } catch (Exception e) {
            log.error("Error comunicandose con AFIP service: {}", e.getMessage());
            throw new RuntimeException("Error al emitir factura en AFIP: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public String consultarUltimoComprobante(Integer puntoVenta, Integer tipoComprobante, Long tenantId) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/afip/ultimo-comprobante")
                            .queryParam("puntoVenta", puntoVenta)
                            .queryParam("tipoComprobante", tipoComprobante)
                            .build())
                    .bodyValue(Map.of("tenantId", tenantId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "ERROR".equals(response.get("status"))) {
                throw new RuntimeException("Error consultando ultimo comprobante: " + response.get("error"));
            }

            return response != null ? response.get("ultimo_comprobante").toString() : "0";
        } catch (Exception e) {
            log.error("Error consultando ultimo comprobante: {}", e.getMessage());
            throw new RuntimeException("Error consultando ultimo comprobante en AFIP: " + e.getMessage(), e);
        }
    }
}
