package com.guidapixel.contable.invoice.web;

import com.guidapixel.contable.invoice.domain.model.Invoice;
import com.guidapixel.contable.invoice.service.InvoiceService;
import com.guidapixel.contable.invoice.web.dto.EmitirRequest;
import com.guidapixel.contable.invoice.web.dto.InvoiceRequest;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final com.guidapixel.contable.invoice.domain.repository.InvoiceRepository invoiceRepository;

    @PostMapping
    public ResponseEntity<Invoice> create(@RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.createInvoice(request));
    }

    @GetMapping("/total-facturado")
    public ResponseEntity<BigDecimal> totalFacturado() {
        return ResponseEntity.ok(invoiceRepository.sumTotalFacturado());
    }

    @GetMapping
    public ResponseEntity<?> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String estado
    ) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No se pudo determinar el tenant"));
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Invoice> invoices = invoiceService.getInvoicesByTenant(tenantId, estado, pageable);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(invoiceService.getInvoiceById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/emitir")
    public ResponseEntity<?> emitir(@RequestBody EmitirRequest request) {
        try {
            Invoice invoice = invoiceService.emitirFactura(request);
            return ResponseEntity.ok(Map.of("status", "OK", "invoice", invoice));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<?> anular(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.anularFactura(id);
            return ResponseEntity.ok(Map.of("status", "OK", "invoice", invoice));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
