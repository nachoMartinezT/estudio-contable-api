package com.guidapixel.contable.ledger.web;

import com.guidapixel.contable.ledger.service.RecurringFeeService;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeOverrideRequest;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeOverrideResponse;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeRequest;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeResponse;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import com.guidapixel.contable.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class RecurringFeeController {

    private final RecurringFeeService recurringFeeService;
    private final JwtService jwtService;

    @GetMapping("/clients/{clientId}/recurring")
    public ResponseEntity<?> getRecurringFee(@PathVariable Long clientId) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            return ResponseEntity.ok(recurringFeeService.getRecurringFee(tenantId, clientId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/clients/{clientId}/recurring")
    public ResponseEntity<?> createOrUpdateRecurringFee(
            @PathVariable Long clientId,
            @RequestBody RecurringFeeRequest request
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            return ResponseEntity.ok(recurringFeeService.createOrUpdateRecurringFee(tenantId, clientId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PutMapping("/clients/{clientId}/recurring")
    public ResponseEntity<?> updateRecurringFee(
            @PathVariable Long clientId,
            @RequestBody RecurringFeeRequest request
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            return ResponseEntity.ok(recurringFeeService.createOrUpdateRecurringFee(tenantId, clientId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @DeleteMapping("/clients/{clientId}/recurring")
    public ResponseEntity<?> deleteRecurringFee(@PathVariable Long clientId) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            recurringFeeService.createOrUpdateRecurringFee(tenantId, clientId, RecurringFeeRequest.builder().active(false).build());
            return ResponseEntity.ok(Map.of("status", "EXITO", "message", "Honorario recurrente desactivado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/clients/{clientId}/recurring/overrides")
    public ResponseEntity<?> getOverrides(@PathVariable Long clientId) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            return ResponseEntity.ok(recurringFeeService.getOverrides(tenantId, clientId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/clients/{clientId}/recurring/overrides")
    public ResponseEntity<?> createOverride(
            @PathVariable Long clientId,
            @RequestBody RecurringFeeOverrideRequest request
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            return ResponseEntity.ok(recurringFeeService.createOverride(tenantId, clientId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/clients/{clientId}/recurring/resolve")
    public ResponseEntity<?> resolveAmount(
            @PathVariable Long clientId,
            @RequestParam String yearMonth
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            BigDecimal amount = recurringFeeService.resolveAmountForMonth(tenantId, clientId, yearMonth);
            return ResponseEntity.ok(Map.of("yearMonth", yearMonth, "amount", amount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/generate-now")
    public ResponseEntity<?> generateNow(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                String role = jwtService.extractRole(jwt);
                if (!"ROLE_ADMIN".equals(role)) {
                    return ResponseEntity.status(403).body(Map.of("status", "ERROR", "error", "Solo ADMIN puede generar honorarios manualmente"));
                }
            }

            String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            if (recurringFeeService.isAlreadyGenerated(tenantId, yearMonth)) {
                return ResponseEntity.status(409).body(Map.of(
                        "status", "ERROR",
                        "error", "Los honorarios de " + yearMonth + " ya fueron generados para este estudio"
                ));
            }

            int generated = recurringFeeService.generateMonthlyFeesForTenant(tenantId, yearMonth);
            return ResponseEntity.ok(Map.of(
                    "status", "EXITO",
                    "generated", generated,
                    "yearMonth", yearMonth
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
