package com.guidapixel.contable.afip.service;

import com.guidapixel.contable.afip.domain.model.Factura;
import com.guidapixel.contable.afip.domain.repository.FacturaRepository;
import com.guidapixel.contable.afip.web.dto.FacturaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AfipFacturacionService {

    @Autowired
    private AfipAuthService authService;

    @Autowired
    private FacturaRepository facturaRepository;

    @Value("${afip.wsfe.url}")
    private String wsfeUrl;

    @Value("${afip.default.cuit-emisor}")
    private String defaultCuitEmisor;

    @Value("${afip.default.punto-venta:1}")
    private Integer defaultPuntoVenta;

    @Value("${afip.default.tipo-comprobante:11}")
    private Integer defaultTipoComprobante;

    private static final DateTimeFormatter AFIP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Obtiene el \u00faltimo n\u00famero de comprobante autorizado para un punto de venta y tipo de comprobante.
     */
    public int obtenerUltimoComprobante(Integer puntoVenta, Integer tipoComprobante, String cuitEmisor) throws Exception {
        Map<String, String> credenciales = authService.getAfipToken();
        String token = credenciales.get("token");
        String sign = credenciales.get("sign");

        String soapXml =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ar:FECompUltimoAutorizado>" +
                        "<ar:Auth>" +
                        "<ar:Token>" + token + "</ar:Token>" +
                        "<ar:Sign>" + sign + "</ar:Sign>" +
                        "<ar:Cuit>" + cuitEmisor + "</ar:Cuit>" +
                        "</ar:Auth>" +
                        "<ar:PtoVta>" + puntoVenta + "</ar:PtoVta>" +
                        "<ar:CbteTipo>" + tipoComprobante + "</ar:CbteTipo>" +
                        "</ar:FECompUltimoAutorizado>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsfeUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error WSFE: " + response.statusCode());
        }

        String cbteNro = extraerEtiqueta(response.body(), "CbteNro");
        if ("No encontrado".equals(cbteNro)) {
            return 0;
        }
        return Integer.parseInt(cbteNro);
    }

    /**
     * Emite una factura electr\u00f3nica solicitando CAE a AFIP.
     */
    public Map<String, Object> emitirFactura(FacturaDto datos, String cuitEmisor) throws Exception {
        String cuit = (cuitEmisor != null && !cuitEmisor.isBlank()) ? cuitEmisor : defaultCuitEmisor;
        Integer ptoVenta = datos.getPuntoVenta() != null ? datos.getPuntoVenta() : defaultPuntoVenta;
        Integer tipoCbte = datos.getTipoComprobante() != null ? datos.getTipoComprobante() : defaultTipoComprobante;

        Map<String, String> credenciales = authService.getAfipToken();
        String token = credenciales.get("token");
        String sign = credenciales.get("sign");

        int ultimoNro = obtenerUltimoComprobante(ptoVenta, tipoCbte, cuit);
        int proximoNro = ultimoNro + 1;

        LocalDate fechaEmision = datos.getFechaEmision() != null ? datos.getFechaEmision() : LocalDate.now();
        String fechaHoy = fechaEmision.format(AFIP_DATE);

        BigDecimal impTotal = calcularImporte(datos);
        BigDecimal impNeto = datos.getImpNeto() != null ? datos.getImpNeto() : impTotal;
        BigDecimal impTotConc = datos.getImpTotConc() != null ? datos.getImpTotConc() : BigDecimal.ZERO;
        BigDecimal impOpEx = datos.getImpOpEx() != null ? datos.getImpOpEx() : BigDecimal.ZERO;
        BigDecimal impTrib = datos.getImpTrib() != null ? datos.getImpTrib() : BigDecimal.ZERO;
        BigDecimal impIVA = datos.getImpIVA() != null ? datos.getImpIVA() : BigDecimal.ZERO;

        Integer concepto = datos.getConcepto() != null ? datos.getConcepto() : 1;
        Integer docTipo = datos.getTipoDocumento() != null ? datos.getTipoDocumento() : 99;
        Long docNro = datos.getNumeroDocumento() != null ? datos.getNumeroDocumento() : 0L;
        Integer condIva = datos.getCondicionIvaReceptorId() != null ? datos.getCondicionIvaReceptorId() : 5;
        String monedaId = datos.getMonedaId() != null ? datos.getMonedaId() : "PES";
        BigDecimal monedaCotiz = datos.getMonedaCotiz() != null ? datos.getMonedaCotiz() : BigDecimal.ONE;

        String fechaServDesde = datos.getFechaServicioDesde() != null ? datos.getFechaServicioDesde().format(AFIP_DATE) : "";
        String fechaServHasta = datos.getFechaServicioHasta() != null ? datos.getFechaServicioHasta().format(AFIP_DATE) : "";
        String fechaVtoPago = datos.getFechaVencimientoPago() != null ? datos.getFechaVencimientoPago().format(AFIP_DATE) : "";

        String soapXml = buildFeCaeSolicitarXml(
                token, sign, cuit,
                ptoVenta, tipoCbte, 1,
                concepto, docTipo, docNro,
                proximoNro, fechaHoy,
                impTotal, impTotConc, impNeto, impOpEx, impTrib, impIVA,
                monedaId, monedaCotiz, condIva,
                fechaServDesde, fechaServHasta, fechaVtoPago
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsfeUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECAESolicitar")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error HTTP AFIP: " + response.statusCode());
        }

        String respuestaXml = response.body();
        String resultado = extraerEtiqueta(respuestaXml, "Resultado");
        String cae = extraerEtiqueta(respuestaXml, "CAE");
        String caeVto = extraerEtiqueta(respuestaXml, "CAEFchVto");
        String observaciones = extraerObservaciones(respuestaXml);

        if (cae == null || "No encontrado".equals(cae) || cae.isEmpty()) {
            throw new RuntimeException("AFIP RECHAZ\u00d3 LA FACTURA: " + (observaciones.isEmpty() ? extraerEtiqueta(respuestaXml, "Msg") : observaciones));
        }

        Factura nuevaFactura = Factura.builder()
                .puntoVenta(ptoVenta)
                .tipoComprobante(tipoCbte)
                .nroComprobante(proximoNro)
                .tipoDocumento(docTipo)
                .numeroDocumento(docNro)
                .nombreCliente(datos.getNombreCliente())
                .condicionIvaReceptorId(condIva)
                .concepto(concepto)
                .fechaEmision(fechaEmision)
                .fechaServicioDesde(datos.getFechaServicioDesde())
                .fechaServicioHasta(datos.getFechaServicioHasta())
                .fechaVencimientoPago(datos.getFechaVencimientoPago())
                .impTotal(impTotal)
                .impTotConc(impTotConc)
                .impNeto(impNeto)
                .impOpEx(impOpEx)
                .impTrib(impTrib)
                .impIVA(impIVA)
                .monedaId(monedaId)
                .monedaCotiz(monedaCotiz)
                .cuitEmisor(cuit)
                .cae(cae)
                .vencimientoCae(LocalDate.parse(caeVto, AFIP_DATE))
                .resultado(resultado)
                .observaciones(observaciones)
                .build();

        facturaRepository.save(nuevaFactura);

        if ("R".equals(resultado)) {
            throw new RuntimeException("Factura RECHAZADA por AFIP: " + observaciones);
        }

        String tipoComprobanteLabel = obtenerTipoComprobanteLabel(tipoCbte);

        return Map.of(
                "estado", "APROBADA",
                "cae", cae,
                "vencimiento_cae", caeVto,
                "tipo_comprobante", tipoComprobanteLabel,
                "punto_venta", ptoVenta,
                "numero_comprobante", proximoNro,
                "cuit_emisor", cuit,
                "imp_total", impTotal.toString(),
                "resultado", resultado
        );
    }

    private String buildFeCaeSolicitarXml(
            String token, String sign, String cuit,
            Integer ptoVenta, Integer tipoCbte, Integer cantReg,
            Integer concepto, Integer docTipo, Long docNro,
            Integer nroComprobante, String fechaHoy,
            BigDecimal impTotal, BigDecimal impTotConc, BigDecimal impNeto,
            BigDecimal impOpEx, BigDecimal impTrib, BigDecimal impIVA,
            String monedaId, BigDecimal monedaCotiz, Integer condIvaReceptorId,
            String fechaServDesde, String fechaServHasta, String fechaVtoPago
    ) {
        StringBuilder xml = new StringBuilder();
        xml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<ar:FECAESolicitar>");
        xml.append("<ar:Auth>");
        xml.append("<ar:Token>").append(token).append("</ar:Token>");
        xml.append("<ar:Sign>").append(sign).append("</ar:Sign>");
        xml.append("<ar:Cuit>").append(cuit).append("</ar:Cuit>");
        xml.append("</ar:Auth>");
        xml.append("<ar:FeCAEReq>");
        xml.append("<ar:FeCabReq>");
        xml.append("<ar:CantReg>").append(cantReg).append("</ar:CantReg>");
        xml.append("<ar:PtoVta>").append(ptoVenta).append("</ar:PtoVta>");
        xml.append("<ar:CbteTipo>").append(tipoCbte).append("</ar:CbteTipo>");
        xml.append("</ar:FeCabReq>");
        xml.append("<ar:FeDetReq>");
        xml.append("<ar:FECAEDetRequest>");
        xml.append("<ar:Concepto>").append(concepto).append("</ar:Concepto>");
        xml.append("<ar:DocTipo>").append(docTipo).append("</ar:DocTipo>");
        xml.append("<ar:DocNro>").append(docNro).append("</ar:DocNro>");
        xml.append("<ar:CbteDesde>").append(nroComprobante).append("</ar:CbteDesde>");
        xml.append("<ar:CbteHasta>").append(nroComprobante).append("</ar:CbteHasta>");
        xml.append("<ar:CbteFch>").append(fechaHoy).append("</ar:CbteFch>");
        xml.append("<ar:ImpTotal>").append(formatDecimal(impTotal)).append("</ar:ImpTotal>");
        xml.append("<ar:ImpTotConc>").append(formatDecimal(impTotConc)).append("</ar:ImpTotConc>");
        xml.append("<ar:ImpNeto>").append(formatDecimal(impNeto)).append("</ar:ImpNeto>");
        xml.append("<ar:ImpOpEx>").append(formatDecimal(impOpEx)).append("</ar:ImpOpEx>");
        xml.append("<ar:ImpTrib>").append(formatDecimal(impTrib)).append("</ar:ImpTrib>");
        xml.append("<ar:ImpIVA>").append(formatDecimal(impIVA)).append("</ar:ImpIVA>");
        xml.append("<ar:MonId>").append(monedaId).append("</ar:MonId>");
        xml.append("<ar:MonCotiz>").append(formatDecimal(monedaCotiz)).append("</ar:MonCotiz>");
        xml.append("<ar:CondicionIVAReceptorId>").append(condIvaReceptorId).append("</ar:CondicionIVAReceptorId>");

        if (fechaServDesde != null && !fechaServDesde.isEmpty()) {
            xml.append("<ar:FchServDesde>").append(fechaServDesde).append("</ar:FchServDesde>");
        }
        if (fechaServHasta != null && !fechaServHasta.isEmpty()) {
            xml.append("<ar:FchServHasta>").append(fechaServHasta).append("</ar:FchServHasta>");
        }
        if (fechaVtoPago != null && !fechaVtoPago.isEmpty()) {
            xml.append("<ar:FchVtoPago>").append(fechaVtoPago).append("</ar:FchVtoPago>");
        }

        xml.append("</ar:FECAEDetRequest>");
        xml.append("</ar:FeDetReq>");
        xml.append("</ar:FeCAEReq>");
        xml.append("</ar:FECAESolicitar>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return xml.toString();
    }

    private BigDecimal calcularImporte(FacturaDto datos) {
        if (datos.getImpTotal() != null) {
            return datos.getImpTotal();
        }
        if (datos.getItems() != null && !datos.getItems().isEmpty()) {
            return datos.getItems().stream()
                    .map(item -> item.getCantidad().multiply(item.getPrecioUnitario()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        throw new IllegalArgumentException("Se requiere impTotal o items para calcular el importe");
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    private String extraerEtiqueta(String xml, String tagName) {
        Matcher m = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">").matcher(xml);
        if (m.find()) return m.group(1);
        return "No encontrado";
    }

    private String extraerObservaciones(String xml) {
        StringBuilder obs = new StringBuilder();
        Matcher m = Pattern.compile("<Observacion>.*?<Msg>(.*?)</Msg>.*?</Observacion>").matcher(xml);
        while (m.find()) {
            if (!obs.isEmpty()) obs.append("; ");
            obs.append(m.group(1));
        }
        return obs.toString();
    }

    private String obtenerTipoComprobanteLabel(Integer tipo) {
        return switch (tipo) {
            case 1 -> "Factura A";
            case 2 -> "Nota de D\u00e9bito A";
            case 3 -> "Nota de Cr\u00e9dito A";
            case 6 -> "Factura B";
            case 7 -> "Nota de D\u00e9bito B";
            case 8 -> "Nota de Cr\u00e9dito B";
            case 11 -> "Factura C";
            case 12 -> "Nota de D\u00e9bito C";
            case 13 -> "Nota de Cr\u00e9dito C";
            default -> "Tipo " + tipo;
        };
    }
}
