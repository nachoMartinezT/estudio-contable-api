package com.guidapixel.contable.ledger.web;

import com.guidapixel.contable.ledger.domain.model.*;
import com.guidapixel.contable.ledger.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/ledger/report-data")
@RequiredArgsConstructor
public class ReportDataController {

    private final AccountMovementRepository movementRepository;
    private final RecurringFeeRepository recurringFeeRepository;
    private final FeeGenerationLogRepository feeGenerationLogRepository;
    private final RecurringFeeOverrideRepository overrideRepository;

    @GetMapping("/account-statement")
    public ResponseEntity<?> getAccountStatementData(
            @RequestParam Long tenantId,
            @RequestParam Long clientId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            List<AccountMovement> movements = movementRepository.findByTenantAndClientAndDateRange(
                    tenantId, clientId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59));
            return ResponseEntity.ok(Map.of("movements", movements));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/all-movements")
    public ResponseEntity<?> getAllMovements(
            @RequestParam Long tenantId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            List<AccountMovement> movements = movementRepository.findByTenantAndDateRange(
                    tenantId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59));
            return ResponseEntity.ok(Map.of("movements", movements));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/active-fees")
    public ResponseEntity<?> getActiveFees(@RequestParam Long tenantId) {
        try {
            List<RecurringFee> fees = recurringFeeRepository.findByTenantIdAndActiveTrue(tenantId);
            return ResponseEntity.ok(Map.of("fees", fees));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/fee-generation-log")
    public ResponseEntity<?> getFeeGenerationLog(
            @RequestParam Long tenantId,
            @RequestParam Long clientId,
            @RequestParam String yearMonth
    ) {
        try {
            List<FeeGenerationLog> logs = feeGenerationLogRepository.findByTenantIdAndClientIdAndYearMonth(
                    tenantId, clientId, yearMonth);
            return ResponseEntity.ok(Map.of("logs", logs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/fee-override")
    public ResponseEntity<?> getFeeOverride(
            @RequestParam Long tenantId,
            @RequestParam Long clientId,
            @RequestParam String yearMonth
    ) {
        try {
            return overrideRepository.findByTenantIdAndClientIdAndYearMonth(tenantId, clientId, yearMonth)
                    .map(override -> ResponseEntity.ok(Map.of("override", override)))
                    .orElse(ResponseEntity.ok(Map.of("override", null)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
