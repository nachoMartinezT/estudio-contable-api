package com.guidapixel.contable.afip.web;

import com.guidapixel.contable.afip.service.AfipAuthService;
import com.guidapixel.contable.afip.service.AfipFacturacionService;
import com.guidapixel.contable.afip.web.dto.FacturaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/afip")
public class AfipController {

    @Autowired
    private AfipAuthService afipAuthService;

    @Autowired
    private AfipFacturacionService afipFacturacionService;

    @Value("${afip.default.cuit-emisor}")
    private String defaultCuitEmisor;

    @GetMapping("/test-token")
    public Map<String, String> testConnection() {
        try {
            Map<String, String> credenciales = afipAuthService.getAfipToken();
            credenciales.put("status", "EXITO");
            credenciales.put("mensaje", "Conexi\u00f3n con AFIP exitosa!");
            return credenciales;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "mensaje", e.getMessage());
        }
    }

    @GetMapping("/ultimo-comprobante")
    public Map<String, Object> consultarUltimo(
            @RequestParam(defaultValue = "1") Integer puntoVenta,
            @RequestParam(defaultValue = "11") Integer tipoComprobante,
            @RequestParam(required = false) String cuitEmisor
    ) {
        try {
            String cuit = (cuitEmisor != null && !cuitEmisor.isBlank()) ? cuitEmisor : defaultCuitEmisor;
            int numero = afipFacturacionService.obtenerUltimoComprobante(puntoVenta, tipoComprobante, cuit);
            return Map.of(
                    "status", "EXITO",
                    "mensaje", "Conexi\u00f3n a WSFE correcta.",
                    "ultimo_comprobante", numero,
                    "punto_venta", puntoVenta,
                    "tipo_comprobante", tipoComprobante,
                    "cuit_emisor", cuit
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    @PostMapping("/emitir")
    public Map<String, Object> emitirFactura(
            @RequestBody FacturaDto facturaDto,
            @RequestParam(required = false) String cuitEmisor
    ) {
        try {
            String cuit = (cuitEmisor != null && !cuitEmisor.isBlank()) ? cuitEmisor : defaultCuitEmisor;
            Map<String, Object> resultado = afipFacturacionService.emitirFactura(facturaDto, cuit);
            return Map.of("status", "EXITO", "datos_factura", resultado);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "mensaje", e.getMessage());
        }
    }
}
