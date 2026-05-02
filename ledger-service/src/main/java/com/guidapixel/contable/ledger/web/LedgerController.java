package com.guidapixel.contable.ledger.web;

import com.guidapixel.contable.ledger.service.LedgerService;
import com.guidapixel.contable.ledger.web.dto.BalanceResponse;
import com.guidapixel.contable.ledger.web.dto.CreateMovementRequest;
import com.guidapixel.contable.ledger.web.dto.MarkPaidRequest;
import com.guidapixel.contable.ledger.web.dto.MovementResponse;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/clients/{clientId}/movements")
    public ResponseEntity<?> getMovements(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return ResponseEntity.ok(ledgerService.getMovements(clientId, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/clients/{clientId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(ledgerService.getBalance(clientId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/clients/{clientId}/movements")
    public ResponseEntity<?> createMovement(
            @PathVariable Long clientId,
            @RequestBody CreateMovementRequest request
    ) {
        try {
            return ResponseEntity.ok(ledgerService.createMovement(clientId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PutMapping("/movements/{id}/mark-paid")
    public ResponseEntity<?> markAsPaid(
            @PathVariable Long id,
            @RequestBody MarkPaidRequest request
    ) {
        try {
            return ResponseEntity.ok(ledgerService.markAsPaid(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/movements/{id}/payment-link")
    public ResponseEntity<?> createPaymentLink(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ledgerService.createPaymentLinkForMovement(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/my/movements")
    public ResponseEntity<?> getMyMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return ResponseEntity.ok(ledgerService.getMyMovements(tenantId, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/my/balance")
    public ResponseEntity<?> getMyBalance() {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            return ResponseEntity.ok(ledgerService.getMyBalance(tenantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
