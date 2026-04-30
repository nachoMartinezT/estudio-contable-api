package com.guidapixel.contable.invoice.web;

import com.guidapixel.contable.invoice.domain.model.Invoice;
import com.guidapixel.contable.invoice.service.InvoiceService;
import com.guidapixel.contable.invoice.web.dto.InvoiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
}
