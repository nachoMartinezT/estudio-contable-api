package com.guidapixel.contable.ledger.web;

import com.guidapixel.contable.ledger.service.LedgerService;
import com.guidapixel.contable.ledger.web.dto.InvoiceMovementRequest;
import com.guidapixel.contable.ledger.web.dto.MovementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/ledger")
@RequiredArgsConstructor
public class InternalLedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/movements/from-invoice")
    public ResponseEntity<?> createMovementFromInvoice(@RequestBody InvoiceMovementRequest request) {
        try {
            MovementResponse response = ledgerService.createMovementFromInvoice(request);
            return ResponseEntity.ok(Map.of("status", "EXITO", "movement", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PutMapping("/movements/mark-paid-by-mp")
    public ResponseEntity<?> markPaidByMp(@RequestBody Map<String, String> request) {
        try {
            String preferenceId = request.get("preferenceId");
            MovementResponse response = ledgerService.markAsPaidByMp(preferenceId);
            return ResponseEntity.ok(Map.of("status", "EXITO", "movement", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/movements/upcoming-due")
    public ResponseEntity<?> getUpcomingDue() {
        try {
            return ResponseEntity.ok(ledgerService.getUpcomingDueMovements());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/movements/overdue")
    public ResponseEntity<?> getOverdue() {
        try {
            return ResponseEntity.ok(ledgerService.getOverdueMovements());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/overdue-reminders/log")
    public ResponseEntity<?> logOverdueReminder(@RequestBody Map<String, Object> request) {
        try {
            Long tenantId = ((Number) request.get("tenantId")).longValue();
            Long clientId = ((Number) request.get("clientId")).longValue();
            Long movementId = ((Number) request.get("movementId")).longValue();
            ledgerService.logOverdueReminder(tenantId, clientId, movementId);
            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
