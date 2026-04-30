package com.guidapixel.contable.invoice.service;

import com.guidapixel.contable.invoice.domain.model.Invoice;
import com.guidapixel.contable.invoice.domain.model.InvoiceItem;
import com.guidapixel.contable.invoice.domain.repository.InvoiceRepository;
import com.guidapixel.contable.invoice.web.dto.InvoiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoice(InvoiceRequest request) {
        Invoice invoice = Invoice.builder()
                .numeroFactura(request.getNumeroFactura())
                .fechaEmision(request.getFechaEmision())
                .clientId(request.getClientId())
                .build();

        BigDecimal totalFactura = BigDecimal.ZERO;

        for (var itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getCantidad().multiply(itemReq.getPrecioUnitario());

            InvoiceItem item = InvoiceItem.builder()
                    .concepto(itemReq.getConcepto())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(itemReq.getPrecioUnitario())
                    .subtotal(subtotal)
                    .build();

            invoice.addItem(item);
            totalFactura = totalFactura.add(subtotal);
        }

        invoice.setTotal(totalFactura);

        return invoiceRepository.save(invoice);
    }
}
