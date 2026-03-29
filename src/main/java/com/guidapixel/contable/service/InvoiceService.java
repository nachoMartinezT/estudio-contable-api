package com.guidapixel.contable.service;

import com.guidapixel.contable.domain.model.Client;
import com.guidapixel.contable.domain.model.Invoice;
import com.guidapixel.contable.domain.model.InvoiceItem;
import com.guidapixel.contable.domain.repository.ClientRepository;
import com.guidapixel.contable.domain.repository.InvoiceRepository;
import com.guidapixel.contable.web.dto.InvoiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Transactional // Importante: Todo o nada. Si falla un ítem, no se guarda la factura.
    public Invoice createInvoice(InvoiceRequest request) {

        // 1. Buscamos el cliente (El filtro automático nos protege de usar clientes ajenos)
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // 2. Creamos la cabecera de la factura
        Invoice invoice = Invoice.builder()
                .numeroFactura(request.getNumeroFactura())
                .fechaEmision(request.getFechaEmision())
                .client(client)
                .build();

        // 3. Procesamos los ítems y calculamos el total
        BigDecimal totalFactura = BigDecimal.ZERO;

        for (var itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getCantidad().multiply(itemReq.getPrecioUnitario());

            InvoiceItem item = InvoiceItem.builder()
                    .concepto(itemReq.getConcepto())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(itemReq.getPrecioUnitario())
                    .subtotal(subtotal)
                    .build();

            // Usamos el método helper para vincularlos bidireccionalmente
            invoice.addItem(item);

            totalFactura = totalFactura.add(subtotal);
        }

        invoice.setTotal(totalFactura);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        auditService.logAction(
                "CREATE_INVOICE",
                "Invoice",
                savedInvoice.getId().toString(),
                "Factura " + savedInvoice.getNumeroFactura() + " creada por $" + savedInvoice.getTotal()
        );

        // 4. Guardamos (El CascadeType.ALL guardará los ítems automáticamente)
        return invoiceRepository.save(invoice);
    }
}