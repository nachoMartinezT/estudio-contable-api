package com.guidapixel.contable.invoice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class LedgerClient {

    private final WebClient webClient;

    public LedgerClient(@Value("${services.ledger-service.url:http://ledger-service:8088}") String ledgerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ledgerUrl)
                .build();
    }

    public void notifyInvoiceCreated(Long tenantId, Long clientId, Long invoiceId, BigDecimal total, String description) {
        try {
            Map<String, Object> request = Map.of(
                    "tenantId", tenantId,
                    "clientId", clientId,
                    "invoiceId", invoiceId,
                    "totalAmount", total,
                    "description", description
            );

            webClient.post()
                    .uri("/api/internal/ledger/movements/from-invoice")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnNext(response -> log.info("Ledger notified for invoice {}: {}", invoiceId, response))
                    .onErrorResume(e -> {
                        log.warn("Could not notify ledger for invoice {}: {}", invoiceId, e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.warn("Error notifying ledger for invoice {}: {}", invoiceId, e.getMessage());
        }
    }
}
