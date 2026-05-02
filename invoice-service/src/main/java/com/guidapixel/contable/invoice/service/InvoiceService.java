package com.guidapixel.contable.invoice.service;

import com.guidapixel.contable.invoice.client.AfipClient;
import com.guidapixel.contable.invoice.client.AfipFacturaRequest;
import com.guidapixel.contable.invoice.client.LedgerClient;
import com.guidapixel.contable.invoice.domain.model.Invoice;
import com.guidapixel.contable.invoice.domain.model.InvoiceItem;
import com.guidapixel.contable.invoice.domain.repository.InvoiceRepository;
import com.guidapixel.contable.invoice.web.dto.InvoiceRequest;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final AfipClient afipClient;
    private final LedgerClient ledgerClient;

    @Transactional
    public Invoice createInvoice(InvoiceRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Invoice invoice = Invoice.builder()
                .numeroFactura(request.getNumeroFactura())
                .fechaEmision(request.getFechaEmision())
                .clientId(request.getClientId())
                .tipoComprobante(request.getTipoComprobante())
                .puntoVenta(request.getPuntoVenta())
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

        if (request.isEmitirAfip()) {
            emitirEnAfip(invoice, request);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        ledgerClient.notifyInvoiceCreated(
                tenantId,
                request.getClientId(),
                savedInvoice.getId(),
                totalFactura,
                "Factura " + (savedInvoice.getNumeroFactura() != null ? savedInvoice.getNumeroFactura() : "interna")
        );

        return savedInvoice;
    }

    private void emitirEnAfip(Invoice invoice, InvoiceRequest request) {
        log.info("Emitiendo factura en AFIP para invoice {}", invoice.getId());

        Long tenantId = TenantContext.getTenantId();

        AfipFacturaRequest afipRequest = AfipFacturaRequest.builder()
                .puntoVenta(request.getPuntoVenta())
                .tipoComprobante(request.getTipoComprobante())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .nombreCliente(request.getNombreCliente())
                .condicionIvaReceptorId(request.getCondicionIvaReceptorId())
                .concepto(request.getConcepto())
                .fechaEmision(request.getFechaEmision())
                .fechaServicioDesde(request.getFechaServicioDesde())
                .fechaServicioHasta(request.getFechaServicioHasta())
                .fechaVencimientoPago(request.getFechaVencimientoPago())
                .impTotal(invoice.getTotal())
                .impTotConc(request.getImpTotConc())
                .impNeto(invoice.getTotal())
                .impOpEx(request.getImpOpEx())
                .impTrib(request.getImpTrib())
                .impIVA(request.getImpIVA())
                .monedaId(request.getMonedaId())
                .monedaCotiz(request.getMonedaCotiz())
                .items(request.getItems().stream()
                        .map(item -> AfipFacturaRequest.AfipItemRequest.builder()
                                .concepto(item.getConcepto())
                                .cantidad(item.getCantidad())
                                .precioUnitario(item.getPrecioUnitario())
                                .build())
                        .toList())
                .build();

        Map<String, Object> resultado = afipClient.emitirFactura(afipRequest, tenantId);

        invoice.setCae((String) resultado.get("cae"));
        invoice.setVencimientoCae(java.time.LocalDate.parse((String) resultado.get("vencimiento_cae")));
        invoice.setNroComprobanteAfip(Integer.parseInt((String) resultado.get("numero_comprobante")));
        invoice.setEstadoAfip((String) resultado.get("estado"));
        invoice.setNumeroFactura(String.format("%04d-%08d",
                invoice.getPuntoVenta(),
                invoice.getNroComprobanteAfip()));

        log.info("Factura emitida en AFIP exitosamente. CAE: {}, Nro: {}",
                invoice.getCae(), invoice.getNumeroFactura());
    }
}
