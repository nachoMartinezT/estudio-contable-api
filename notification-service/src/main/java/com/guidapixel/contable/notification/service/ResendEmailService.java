package com.guidapixel.contable.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendEmailService {

    private final RestTemplate restTemplate;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:noreply@guidapixel.com}")
    private String fromEmail;

    private static final String RESEND_URL = "https://api.resend.com/emails";

    public void sendEmail(String toEmail, String toName, String subject, String htmlContent, String tenantName) {
        String from = tenantName + " via Guida Contable <" + fromEmail + ">";

        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(toEmail),
                "subject", subject,
                "html", htmlContent
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + resendApiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    RESEND_URL,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email enviado a {} con asunto: {}", toEmail, subject);
            } else {
                log.error("Error enviando email a {}: {}", toEmail, response.getBody());
            }
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email via Resend", e);
        }
    }

    public String buildBienvenidaUsuario(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreUsuario = vars.getOrDefault("nombreUsuario", "Usuario");
        String email = vars.getOrDefault("email", "");
        String password = vars.getOrDefault("passwordTemporal", "");
        String loginUrl = vars.getOrDefault("loginUrl", "https://app.guidapixel.com");

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#1e40af;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Bienvenido a %s</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Tu cuenta ha sido creada en el sistema de <strong>%s</strong>. A continuacion te compartimos tus credenciales de acceso:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Email</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Password temporal</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                </table>
                                <p style="margin-top:16px;">Por seguridad, te recomendamos cambiar tu password en el primer inicio de sesion.</p>
                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;">
                                    <tr><td align="center">
                                        <a href="%s" style="background-color:#1e40af;color:#ffffff;padding:12px 32px;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;">Iniciar Sesion</a>
                                    </td></tr>
                                </table>
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreEstudio, nombreUsuario, nombreEstudio, email, password, loginUrl);
    }

    public String buildLinkPagoMp(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreCliente = vars.getOrDefault("nombreCliente", "Cliente");
        String monto = vars.getOrDefault("monto", "0");
        String descripcion = vars.getOrDefault("descripcion", "");
        String fechaVencimiento = vars.getOrDefault("fechaVencimiento", "");
        String paymentLinkUrl = vars.getOrDefault("paymentLinkUrl", "");

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#1e40af;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Tenés un pago pendiente con %s</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Te informamos que tenés un pago pendiente:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Concepto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Monto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">$%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Vencimiento</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                </table>
                                <p>Podés realizar el pago de forma segura a traves de MercadoPago haciendo clic en el siguiente boton:</p>
                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;">
                                    <tr><td align="center">
                                        <a href="%s" style="background-color:#009ee3;color:#ffffff;padding:12px 32px;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;">Pagar con MercadoPago</a>
                                    </td></tr>
                                </table>
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreEstudio, nombreCliente, descripcion, monto, fechaVencimiento, paymentLinkUrl);
    }

    public String buildPagoConfirmado(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreCliente = vars.getOrDefault("nombreCliente", "Cliente");
        String monto = vars.getOrDefault("monto", "0");
        String descripcion = vars.getOrDefault("descripcion", "");
        String fechaPago = vars.getOrDefault("fechaPago", "");

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#059669;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Pago recibido</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Te confirmamos que recibimos tu pago:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#f0fdf4;border:1px solid #bbf7d0;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Concepto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Monto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">$%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Fecha de pago</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                </table>
                                <p>Tu cuenta se encuentra actualizada. Gracias por tu pago.</p>
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreCliente, descripcion, monto, fechaPago);
    }

    public String buildVencimientoProximo(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreCliente = vars.getOrDefault("nombreCliente", "Cliente");
        String monto = vars.getOrDefault("monto", "0");
        String descripcion = vars.getOrDefault("descripcion", "");
        String fechaVencimiento = vars.getOrDefault("fechaVencimiento", "");
        String diasRestantes = vars.getOrDefault("diasRestantes", "0");
        String paymentLinkUrl = vars.get("paymentLinkUrl");

        String ctaButton = "";
        if (paymentLinkUrl != null && !paymentLinkUrl.isBlank()) {
            ctaButton = """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;">
                    <tr><td align="center">
                        <a href="%s" style="background-color:#1e40af;color:#ffffff;padding:12px 32px;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;">Pagar ahora</a>
                    </td></tr>
                </table>
                """.formatted(paymentLinkUrl);
        }

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#d97706;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Recordatorio de pago</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Te recordamos que el siguiente pago vence en <strong>%s dias</strong>:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#fffbeb;border:1px solid #fde68a;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Concepto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Monto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">$%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Vencimiento</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                </table>
                                %s
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreCliente, diasRestantes, descripcion, monto, fechaVencimiento, ctaButton);
    }

    public String buildDocumentoCompartido(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreCliente = vars.getOrDefault("nombreCliente", "Cliente");
        String nombreArchivo = vars.getOrDefault("nombreArchivo", "");
        String categoria = vars.getOrDefault("categoria", "");
        String descripcion = vars.getOrDefault("descripcion", "");
        String portalUrl = vars.getOrDefault("portalUrl", "https://app.guidapixel.com/documentos");

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#7c3aed;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Documento compartido</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p><strong>%s</strong> compartio un documento con vos:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Archivo</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Categoria</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Descripcion</td><td style="color:#1e293b;font-size:14px;">%s</td></tr>
                                </table>
                                <p>Podes ver y descargar el documento desde el portal:</p>
                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;">
                                    <tr><td align="center">
                                        <a href="%s" style="background-color:#7c3aed;color:#ffffff;padding:12px 32px;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;">Ver Documento</a>
                                    </td></tr>
                                </table>
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreCliente, nombreEstudio, nombreArchivo, categoria, descripcion, portalUrl);
    }

    public String buildHonorarioGenerado(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreCliente = vars.getOrDefault("nombreCliente", "Cliente");
        String monto = vars.getOrDefault("monto", "0");
        String mes = vars.getOrDefault("mes", "");
        String fechaVencimiento = vars.getOrDefault("fechaVencimiento", "");
        String portalUrl = vars.getOrDefault("portalUrl", "https://app.guidapixel.com/mi-cuenta");

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#1e40af;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Honorarios %s</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Te informamos que se generaron los honorarios correspondientes al mes de <strong>%s</strong>:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Concepto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">Honorarios profesionales</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Monto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">$%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Vencimiento</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                </table>
                                <p>Podes ver el detalle de tu cuenta y realizar el pago desde el portal:</p>
                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;">
                                    <tr><td align="center">
                                        <a href="%s" style="background-color:#1e40af;color:#ffffff;padding:12px 32px;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;">Ver mi cuenta</a>
                                    </td></tr>
                                </table>
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(mes, nombreCliente, mes, monto, fechaVencimiento, portalUrl);
    }

    public String buildDeudaVencida(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreCliente = vars.getOrDefault("nombreCliente", "Cliente");
        String monto = vars.getOrDefault("monto", "0");
        String descripcion = vars.getOrDefault("descripcion", "");
        String fechaVencimiento = vars.getOrDefault("fechaVencimiento", "");
        String diasVencido = vars.getOrDefault("diasVencido", "0");
        String paymentLinkUrl = vars.get("paymentLinkUrl");

        String ctaButton = "";
        if (paymentLinkUrl != null && !paymentLinkUrl.isBlank()) {
            ctaButton = """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;">
                    <tr><td align="center">
                        <a href="%s" style="background-color:#dc2626;color:#ffffff;padding:12px 32px;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;">Regularizar pago</a>
                    </td></tr>
                </table>
                """.formatted(paymentLinkUrl);
        }

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#dc2626;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Pago vencido</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>Te informamos que el siguiente pago se encuentra <strong style="color:#dc2626;">vencido</strong>:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#fef2f2;border:1px solid #fecaca;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#64748b;font-size:13px;">Concepto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Monto</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">$%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Fecha de vencimiento</td><td style="color:#1e293b;font-weight:bold;font-size:14px;">%s</td></tr>
                                    <tr><td style="color:#64748b;font-size:13px;">Estado</td><td style="color:#dc2626;font-weight:bold;font-size:14px;">Vencido hace %s dias</td></tr>
                                </table>
                                <p>Te solicitamos que regularices tu pago a la brevedad para evitar inconvenientes.</p>
                                %s
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreCliente, descripcion, monto, fechaVencimiento, diasVencido, ctaButton);
    }

    public String buildResumenDeudaVencida(Map<String, String> vars) {
        String nombreEstudio = vars.getOrDefault("nombreEstudio", "Estudio");
        String nombreAdmin = vars.getOrDefault("nombreAdmin", "Admin");
        String totalVencido = vars.getOrDefault("totalVencido", "0");
        String listaClientes = vars.getOrDefault("listaClientes", "");

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.1);">
                            <tr><td style="background-color:#dc2626;padding:24px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:22px;">Resumen semanal de deuda vencida</h1>
                            </td></tr>
                            <tr><td style="padding:32px 24px;color:#333333;font-size:15px;line-height:1.6;">
                                <p>Hola <strong>%s</strong>,</p>
                                <p>A continuacion el resumen de deuda vencida de <strong>%s</strong> correspondiente a esta semana:</p>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#fef2f2;border:1px solid #fecaca;border-radius:6px;margin:16px 0;">
                                    <tr>
                                        <th style="color:#dc2626;font-size:13px;text-align:left;padding:8px;border-bottom:1px solid #fecaca;">Cliente</th>
                                        <th style="color:#dc2626;font-size:13px;text-align:left;padding:8px;border-bottom:1px solid #fecaca;">Monto vencido</th>
                                        <th style="color:#dc2626;font-size:13px;text-align:left;padding:8px;border-bottom:1px solid #fecaca;">Dias vencido</th>
                                    </tr>
                                    %s
                                </table>
                                <table width="100%%" cellpadding="12" cellspacing="0" style="background-color:#fef2f2;border:1px solid #fecaca;border-radius:6px;margin:16px 0;">
                                    <tr><td style="color:#dc2626;font-weight:bold;font-size:14px;">Total vencido</td><td style="color:#dc2626;font-weight:bold;font-size:16px;text-align:right;">$%s</td></tr>
                                </table>
                                <p style="color:#64748b;font-size:13px;">Los recordatorios automaticos fueron enviados a los clientes listados arriba.</p>
                            </td></tr>
                            <tr><td style="background-color:#f8fafc;padding:16px;text-align:center;color:#94a3b8;font-size:12px;border-top:1px solid #e2e8f0;">
                                Powered by Guida Contable | guidapixel.com
                            </td></tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(nombreAdmin, nombreEstudio, listaClientes, totalVencido);
    }
}
