package com.guidapixel.contable.invoice.service;

import com.guidapixel.contable.invoice.client.AfipClient;
import com.guidapixel.contable.invoice.client.AfipFacturaRequest;
import com.guidapixel.contable.invoice.client.ClientInfoClient;
import com.guidapixel.contable.invoice.client.LedgerClient;
import com.guidapixel.contable.invoice.client.NotificationClient;
import com.guidapixel.contable.invoice.domain.model.Invoice;
import com.guidapixel.contable.invoice.domain.model.InvoiceItem;
import com.guidapixel.contable.invoice.domain.repository.InvoiceRepository;
import com.guidapixel.contable.invoice.web.dto.InvoiceRequest;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter AFIP_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final InvoiceRepository invoiceRepository;
    private final AfipClient afipClient;
    private final LedgerClient ledgerClient;
    private final NotificationClient notificationClient;
    private final ClientInfoClient clientInfoClient;

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
            sendInvoiceEmail(invoice, request, tenantId);
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
        invoice.setVencimientoCae(parseAfipDate(resultado.get("vencimiento_cae")));
        invoice.setNroComprobanteAfip(toInteger(resultado.get("numero_comprobante")));
        invoice.setEstadoAfip((String) resultado.get("estado"));
        invoice.setEstado("EMITIDA_AFIP");
        invoice.setNumeroFactura(String.format("%04d-%08d",
                invoice.getPuntoVenta(),
                invoice.getNroComprobanteAfip()));

        log.info("Factura emitida en AFIP exitosamente. CAE: {}, Nro: {}",
                invoice.getCae(), invoice.getNumeroFactura());
    }

    private LocalDate parseAfipDate(Object value) {
        if (value == null) {
            throw new RuntimeException("AFIP no informo vencimiento de CAE");
        }
        return LocalDate.parse(value.toString(), AFIP_DATE);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        throw new RuntimeException("AFIP no informo numero de comprobante");
    }

    public Page<Invoice> getInvoicesByTenant(Long tenantId, String estado, Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return invoiceRepository.findByTenantIdAndEstado(tenantId, estado, pageable);
        }
        return invoiceRepository.findByTenantId(tenantId, pageable);
    }

    public Invoice getInvoiceById(Long invoiceId) {
        Long tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        if (!invoice.getTenantId().equals(tenantId)) {
            throw new RuntimeException("No tienes acceso a esta factura");
        }
        return invoice;
    }

    @Transactional
    public Invoice emitirFactura(Long invoiceId) {
        Long tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        if (!invoice.getTenantId().equals(tenantId)) {
            throw new RuntimeException("No tienes acceso a esta factura");
        }
        if (invoice.isEmitidaAfip()) {
            throw new RuntimeException("La factura ya fue emitida a AFIP");
        }

        emitirEnAfip(invoice, toInvoiceRequest(invoice));
        invoice.setEstado("EMITIDA_AFIP");
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice anularFactura(Long invoiceId) {
        Long tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        if (!invoice.getTenantId().equals(tenantId)) {
            throw new RuntimeException("No tienes acceso a esta factura");
        }
        if (invoice.isEmitidaAfip()) {
            throw new RuntimeException("No se puede anular una factura ya emitida a AFIP");
        }
        invoice.setEstado("ANULADA");
        return invoiceRepository.save(invoice);
    }

    private InvoiceRequest toInvoiceRequest(Invoice invoice) {
        InvoiceRequest request = new InvoiceRequest();
        request.setClientId(invoice.getClientId());
        request.setNumeroFactura(invoice.getNumeroFactura());
        request.setFechaEmision(invoice.getFechaEmision());
        request.setTipoComprobante(invoice.getTipoComprobante());
        request.setPuntoVenta(invoice.getPuntoVenta());
        request.setItems(invoice.getItems().stream().map(item -> {
            var itemReq = new com.guidapixel.contable.invoice.web.dto.InvoiceItemRequest();
            itemReq.setConcepto(item.getConcepto());
            itemReq.setCantidad(item.getCantidad());
            itemReq.setPrecioUnitario(item.getPrecioUnitario());
            return itemReq;
        }).toList());
        return request;
    }
}
