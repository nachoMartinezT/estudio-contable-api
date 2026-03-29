package com.guidapixel.contable.web.controller;

import com.guidapixel.contable.service.afip.AfipAuthService;
import com.guidapixel.contable.service.afip.AfipFacturacionService;
import com.guidapixel.contable.web.dto.FacturaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/afip")
public class AfipTestController {

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
                    "mensaje", "Conexión a WSFE correcta.",
                    "ultimo_comprobante_c", numero
            );
        } catch (Exception e) {
            e.printStackTrace(); // Mirar consola si falla
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    @GetMapping("/test-token")
    public Map<String, String> testConnection() {
        try {
            Map<String, String> credenciales = afipAuthService.getAfipToken();
            credenciales.put("status", "EXITO");
            credenciales.put("mensaje", "¡Conexión con AFIP exitosa! Tenemos credenciales.");
            return credenciales;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "status", "ERROR",
                    "mensaje", e.getMessage()
            );
        }
    }

    @PostMapping("/emitir")
    public Map<String, Object> emitirFactura(@RequestBody FacturaDto facturaDto) {
        try {
            Map<String, String> resultado = afipFacturacionService.emitirFactura(facturaDto);
            return Map.of(
                    "status", "EXITO",
                    "datos_factura", resultado
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "status", "ERROR",
                    "mensaje", e.getMessage()
            );
        }
    }
}