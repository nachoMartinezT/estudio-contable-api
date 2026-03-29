package com.guidapixel.contable.web.controller;

import com.guidapixel.contable.domain.model.Invoice;
import com.guidapixel.contable.service.InvoiceService;
import com.guidapixel.contable.web.dto.InvoiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<Invoice> create(@RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.createInvoice(request));
    }
}