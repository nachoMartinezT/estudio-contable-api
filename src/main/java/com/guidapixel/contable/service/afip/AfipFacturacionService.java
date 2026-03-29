package com.guidapixel.contable.service.afip;

import com.guidapixel.contable.domain.model.Factura;
import com.guidapixel.contable.domain.repository.FacturaRepository;
import com.guidapixel.contable.web.dto.FacturaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
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

    // CUIT del emisor (CUIT de testing que salió en el log)
    private static final String CUIT_EMISOR = "20325847094";

    public String obtenerUltimoComprobante() throws Exception {
        // 1. Conseguimos las credenciales (Token y Sign)
        Map<String, String> credenciales = authService.getAfipToken();
        String token = credenciales.get("token");
        String sign = credenciales.get("sign");

        // 2. Armamos el XML para preguntar "FECompUltimoAutorizado"
        // PtoVta 1, CbteTipo 11 (Factura C)
        String soapXml =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ar:FECompUltimoAutorizado>" +
                        "<ar:Auth>" +
                        "<ar:Token>" + token + "</ar:Token>" +
                        "<ar:Sign>" + sign + "</ar:Sign>" +
                        "<ar:Cuit>" + CUIT_EMISOR + "</ar:Cuit>" +
                        "</ar:Auth>" +
                        "<ar:PtoVta>1</ar:PtoVta>" +
                        "<ar:CbteTipo>11</ar:CbteTipo>" + // 11 = Factura C, 6 = Factura B, 1 = Factura A
                        "</ar:FECompUltimoAutorizado>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        // 3. Enviamos a AFIP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsfeUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Leemos la respuesta
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error WSFE: " + response.statusCode());
        }

        return extraerNumero(response.body());
    }

    private String extraerNumero(String xml) {
        // Buscamos el tag <CbteNro>
        Matcher m = Pattern.compile("<CbteNro>(.*?)</CbteNro>").matcher(xml);
        if (m.find()) {
            return m.group(1);
        }
        return "No se encontró (¿Quizás es la primera factura? Respondió: " + xml + ")";
    }

    public Map<String, String> emitirFactura(FacturaDto datos) throws Exception {
        // 1. Obtener Token y Sign
        Map<String, String> credenciales = authService.getAfipToken();
        String token = credenciales.get("token");
        String sign = credenciales.get("sign");

        // 2. Averiguar qué número de factura toca (La última + 1)
        int ultimoNro = Integer.parseInt(obtenerUltimoComprobante());
        int proximoNro = ultimoNro + 1;

        // 3. Preparar datos
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")); // Formato AAAAMMDD
        String importeString = String.format(Locale.US, "%.2f", datos.getMonto()); // Ej: 100.00 (con punto, no coma)

        // DocTipo: 96=DNI, 80=CUIT, 99=Consumidor Final (Sin identificar)
        String docTipo = (datos.getDniCliente() != null && datos.getDniCliente() > 0) ? "96" : "99";
        String docNro = (datos.getDniCliente() != null && datos.getDniCliente() > 0) ? String.valueOf(datos.getDniCliente()) : "0";

        String soapXml =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ar:FECAESolicitar>" +
                        "<ar:Auth>" +
                        "<ar:Token>" + token + "</ar:Token>" +
                        "<ar:Sign>" + sign + "</ar:Sign>" +
                        "<ar:Cuit>" + CUIT_EMISOR + "</ar:Cuit>" +
                        "</ar:Auth>" +
                        "<ar:FeCAEReq>" +
                        "<ar:FeCabReq>" +
                        "<ar:CantReg>1</ar:CantReg>" +
                        "<ar:PtoVta>1</ar:PtoVta>" +
                        "<ar:CbteTipo>11</ar:CbteTipo>" +
                        "</ar:FeCabReq>" +
                        "<ar:FeDetReq>" +
                        "<ar:FECAEDetRequest>" +
                        "<ar:Concepto>1</ar:Concepto>" +
                        "<ar:DocTipo>" + docTipo + "</ar:DocTipo>" +
                        "<ar:DocNro>" + docNro + "</ar:DocNro>" +
                        "<ar:CbteDesde>" + proximoNro + "</ar:CbteDesde>" +
                        "<ar:CbteHasta>" + proximoNro + "</ar:CbteHasta>" +
                        "<ar:CbteFch>" + fechaHoy + "</ar:CbteFch>" +
                        "<ar:ImpTotal>" + importeString + "</ar:ImpTotal>" +
                        "<ar:ImpTotConc>0</ar:ImpTotConc>" +
                        "<ar:ImpNeto>" + importeString + "</ar:ImpNeto>" +
                        "<ar:ImpOpEx>0</ar:ImpOpEx>" +
                        "<ar:ImpTrib>0</ar:ImpTrib>" +
                        "<ar:ImpIVA>0</ar:ImpIVA>" +
                        "<ar:MonId>PES</ar:MonId>" +
                        "<ar:MonCotiz>1</ar:MonCotiz>" +
                        // --- 👇 NUEVO: Condición IVA del Receptor (5 = Consumidor Final) 👇 ---
                        "<ar:CondicionIVAReceptorId>5</ar:CondicionIVAReceptorId>" +
                        "</ar:FECAEDetRequest>" +
                        "</ar:FeDetReq>" +
                        "</ar:FeCAEReq>" +
                        "</ar:FECAESolicitar>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        // 5. Enviar a AFIP
        System.out.println("---- ENVIANDO FACTURA " + proximoNro + " POR $" + importeString + " ----");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wsfeUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECAESolicitar")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


        // 6. Analizar respuesta
        if (response.statusCode() != 200) {
            System.out.println("❌ ERROR HTTP AFIP: " + response.body());
            throw new RuntimeException("Error HTTP: " + response.statusCode());
        }

        String respuestaXml = response.body();
        System.out.println("📦 RESPUESTA CRUDA DE AFIP: " + respuestaXml);

        // Buscar el CAE y el Resultado (A=Aprobado, R=Rechazado)
        String resultado = extraerEtiqueta(respuestaXml, "Resultado");
        String cae = extraerEtiqueta(respuestaXml, "CAE");
        String caeVto = extraerEtiqueta(respuestaXml, "CAEFchVto");

        if (cae == null || "No encontrado".equals(cae) || cae.isEmpty()) {
            // Buscamos el error real dentro de <Msg>
            String errorMsg = extraerEtiqueta(respuestaXml, "Msg");
            throw new RuntimeException("❌ AFIP RECHAZÓ LA FACTURA: " + errorMsg);
        }

        Factura nuevaFactura = new Factura();
        nuevaFactura.setNroComprobante(proximoNro);
        nuevaFactura.setMonto(datos.getMonto());
        nuevaFactura.setDniCliente(datos.getDniCliente());
        nuevaFactura.setCae(cae);
        nuevaFactura.setVencimientoCae(caeVto);
        nuevaFactura.setFechaEmision(LocalDateTime.now());

        facturaRepository.save(nuevaFactura); // Magia de Spring ✨
        System.out.println("✅ Factura N° " + proximoNro + " guardada en base de datos.");

        if ("R".equals(resultado)) {
            // Si fue rechazado, buscamos el motivo
            String errorMsg = extraerEtiqueta(respuestaXml, "Msg");
            throw new RuntimeException("Factura RECHAZADA por AFIP: " + errorMsg);
        }

        return Map.of(
                "estado", "APROBADA",
                "cae", cae,
                "vencimiento_cae", caeVto,
                "numero_comprobante", String.valueOf(proximoNro),
                "monto", importeString
        );
    }

    // Helper para sacar datos del XML con Regex rápido
    private String extraerEtiqueta(String xml, String tagName) {
        Matcher m = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">").matcher(xml);
        if (m.find()) return m.group(1);
        return "No encontrado";
    }
}