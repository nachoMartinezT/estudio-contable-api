package com.guidapixel.contable.afip.web;

import com.guidapixel.contable.afip.service.AfipAuthService;
import com.guidapixel.contable.afip.service.AfipFacturacionService;
import com.guidapixel.contable.afip.web.dto.FacturaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/afip")
public class AfipController {

    @Autowired
    private AfipAuthService afipAuthService;

    @Autowired
    private AfipFacturacionService afipFacturacionService;

    @GetMapping("/ultimo-comprobante")
    public Map<String, String> consultarUltimo() {
        try {
            String numero = afipFacturacionService.obtenerUltimoComprobante();
            return Map.of(
                    "status", "EXITO",
                    "mensaje", "Conexi\u00f3n a WSFE correcta.",
                    "ultimo_comprobante_c", numero
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

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

    @PostMapping("/emitir")
    public Map<String, Object> emitirFactura(@RequestBody FacturaDto facturaDto) {
        try {
            Map<String, String> resultado = afipFacturacionService.emitirFactura(facturaDto);
            return Map.of("status", "EXITO", "datos_factura", resultado);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "mensaje", e.getMessage());
        }
    }
}
