package com.guidapixel.contable.invoice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    public Map<String, Object> emitirFactura(AfipFacturaRequest request) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/api/afip/emitir")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "ERROR".equals(response.get("status"))) {
                throw new RuntimeException("AFIP rechaz\u00f3 la factura: " + response.get("mensaje"));
            }

            return (Map<String, Object>) response.get("datos_factura");
        } catch (Exception e) {
            log.error("Error comunic\u00e1ndose con AFIP service: {}", e.getMessage());
            throw new RuntimeException("Error al emitir factura en AFIP: " + e.getMessage(), e);
        }
    }

    public String consultarUltimoComprobante(Integer puntoVenta, Integer tipoComprobante) {
        try {
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/afip/ultimo-comprobante")
                            .queryParam("puntoVenta", puntoVenta)
                            .queryParam("tipoComprobante", tipoComprobante)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "ERROR".equals(response.get("status"))) {
                throw new RuntimeException("Error consultando \u00faltimo comprobante: " + response.get("error"));
            }

            return response != null ? response.get("ultimo_comprobante") : "0";
        } catch (Exception e) {
            log.error("Error consultando \u00faltimo comprobante: {}", e.getMessage());
            throw new RuntimeException("Error consultando \u00faltimo comprobante en AFIP: " + e.getMessage(), e);
        }
    }
}
