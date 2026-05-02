package com.guidapixel.contable.afip.web;

import com.guidapixel.contable.afip.client.AuthTenantClient;
import com.guidapixel.contable.afip.service.AfipAuthService;
import com.guidapixel.contable.afip.service.AfipFacturacionService;
import com.guidapixel.contable.afip.web.dto.FacturaDto;
import com.guidapixel.contable.shared.model.TenantAfipConfig;
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

    @Autowired
    private AuthTenantClient authTenantClient;

    @PostMapping("/test-token")
    public Map<String, String> testConnection(@RequestBody TenantIdRequest request) {
        try {
            TenantAfipConfig tenantConfig = authTenantClient.getTenantAfipConfig(request.getTenantId());
            Map<String, String> credenciales = afipAuthService.getAfipToken(tenantConfig);
            credenciales.put("status", "EXITO");
            credenciales.put("mensaje", "Conexion con AFIP exitosa!");
            return credenciales;
        } catch (Exception e) {
            return Map.of("status", "ERROR", "mensaje", e.getMessage());
        }
    }

    @PostMapping("/ultimo-comprobante")
    public Map<String, Object> consultarUltimo(
            @RequestParam(defaultValue = "1") Integer puntoVenta,
            @RequestParam(defaultValue = "11") Integer tipoComprobante,
            @RequestBody TenantIdRequest request
    ) {
        try {
            TenantAfipConfig tenantConfig = authTenantClient.getTenantAfipConfig(request.getTenantId());
            int numero = afipFacturacionService.obtenerUltimoComprobante(
                    puntoVenta, tipoComprobante, tenantConfig.getAfipCuit(), tenantConfig);
            return Map.of(
                    "status", "EXITO",
                    "mensaje", "Conexion a WSFE correcta.",
                    "ultimo_comprobante", numero,
                    "punto_venta", puntoVenta,
                    "tipo_comprobante", tipoComprobante,
                    "cuit_emisor", tenantConfig.getAfipCuit()
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    @PostMapping("/emitir")
    public Map<String, Object> emitirFactura(
            @RequestBody FacturaEmitirRequest request
    ) {
        try {
            TenantAfipConfig tenantConfig = authTenantClient.getTenantAfipConfig(request.getTenantId());
            Map<String, Object> resultado = afipFacturacionService.emitirFactura(
                    request.getFactura(), tenantConfig);
            return Map.of("status", "EXITO", "datos_factura", resultado);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "mensaje", e.getMessage());
        }
    }
}
