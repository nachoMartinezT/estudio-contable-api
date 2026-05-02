package com.guidapixel.contable.ledger.web;

import com.guidapixel.contable.ledger.service.ReportService;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import com.guidapixel.contable.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ledger/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final JwtService jwtService;

    @GetMapping("/clients/{clientId}/account-statement")
    public ResponseEntity<?> getAccountStatement(
            @PathVariable Long clientId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }

            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);

            byte[] content;
            String filename;
            MediaType contentType;

            if ("excel".equalsIgnoreCase(format)) {
                content = reportService.generateAccountStatementExcel(tenantId, clientId, fromDate, toDate);
                filename = "estado_cuenta_" + from + "_" + to + ".xlsx";
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                content = reportService.generateAccountStatementPdf(tenantId, clientId, fromDate, toDate);
                filename = "estado_cuenta_" + from + "_" + to + ".pdf";
                contentType = MediaType.APPLICATION_PDF;
            }

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/fees/period-summary")
    public ResponseEntity<?> getFeePeriodSummary(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }

            byte[] content;
            String filename;
            MediaType contentType;

            if ("excel".equalsIgnoreCase(format)) {
                content = reportService.generateFeePeriodSummaryExcel(tenantId, yearMonth);
                filename = "honorarios_" + yearMonth + ".xlsx";
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                content = reportService.generateFeePeriodSummaryPdf(tenantId, yearMonth);
                filename = "honorarios_" + yearMonth + ".pdf";
                contentType = MediaType.APPLICATION_PDF;
            }

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/studio/income-summary")
    public ResponseEntity<?> getIncomeSummary(
            @RequestParam String from,
            @RequestParam String to,
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
                if (!"ROLE_ADMIN".equals(role) && !"ROLE_SUPER_ADMIN".equals(role)) {
                    return ResponseEntity.status(403).body(Map.of(
                            "status", "ERROR", "error", "Solo ADMIN puede generar resumen de ingresos"));
                }
            } else {
                return ResponseEntity.status(401).body(Map.of("status", "ERROR", "error", "Autenticación requerida"));
            }

            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);

            byte[] content = reportService.generateIncomeSummaryPdf(tenantId, fromDate, toDate);
            String filename = "resumen_ingresos_" + from + "_" + to + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
