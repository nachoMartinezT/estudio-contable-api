package com.guidapixel.contable.notification.service;

import com.guidapixel.contable.notification.domain.model.NotificationLog;
import com.guidapixel.contable.notification.domain.repository.NotificationLogRepository;
import com.guidapixel.contable.notification.web.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ResendEmailService emailService;
    private final NotificationLogRepository logRepository;

    public void sendNotification(SendNotificationRequest request) {
        String subject = "";
        String htmlContent = "";

        switch (request.getTemplateType()) {
            case "BIENVENIDA_USUARIO" -> {
                subject = "Bienvenido a " + request.getTenantName() + " - Tus credenciales de acceso";
                htmlContent = emailService.buildBienvenidaUsuario(request.getVariables());
            }
            case "LINK_PAGO_MP" -> {
                subject = "Tenes un pago pendiente con " + request.getTenantName();
                htmlContent = emailService.buildLinkPagoMp(request.getVariables());
            }
            case "PAGO_CONFIRMADO" -> {
                subject = "Pago recibido - " + request.getTenantName();
                htmlContent = emailService.buildPagoConfirmado(request.getVariables());
            }
            case "VENCIMIENTO_PROXIMO" -> {
                String dias = request.getVariables().getOrDefault("diasRestantes", "0");
                subject = "Recordatorio de pago - vence en " + dias + " dias";
                htmlContent = emailService.buildVencimientoProximo(request.getVariables());
            }
            case "DOCUMENTO_COMPARTIDO" -> {
                subject = request.getTenantName() + " compartio un documento con vos";
                htmlContent = emailService.buildDocumentoCompartido(request.getVariables());
            }
            case "HONORARIO_GENERADO" -> {
                String mes = request.getVariables().getOrDefault("mes", "");
                subject = "Honorarios " + mes + " - " + request.getTenantName();
                htmlContent = emailService.buildHonorarioGenerado(request.getVariables());
            }
            case "DEUDA_VENCIDA" -> {
                String dias = request.getVariables().getOrDefault("diasVencido", "0");
                subject = "Pago vencido - " + request.getTenantName();
                htmlContent = emailService.buildDeudaVencida(request.getVariables());
            }
            case "RESUMEN_DEUDA_VENCIDA" -> {
                subject = "Resumen semanal de deuda vencida - " + request.getTenantName();
                htmlContent = emailService.buildResumenDeudaVencida(request.getVariables());
            }
            default -> throw new IllegalArgumentException("Template desconocido: " + request.getTemplateType());
        }

        NotificationLog logEntry = NotificationLog.builder()
                .tenantId(request.getTenantId())
                .toEmail(request.getToEmail())
                .templateType(request.getTemplateType())
                .sentAt(LocalDateTime.now())
                .build();

        try {
            emailService.sendEmail(
                    request.getToEmail(),
                    request.getToName(),
                    subject,
                    htmlContent,
                    request.getTenantName()
            );
            logEntry.setSuccess(true);
        } catch (Exception e) {
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Error enviando notificacion {} a {}: {}", request.getTemplateType(), request.getToEmail(), e.getMessage());
        }

        logRepository.save(logEntry);
    }

    public void sendVencimientoProximo(Map<String, String> vars, String toEmail, String toName, String tenantName, Long tenantId) {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .templateType("VENCIMIENTO_PROXIMO")
                .toEmail(toEmail)
                .toName(toName)
                .tenantName(tenantName)
                .tenantId(tenantId)
                .variables(vars)
                .build();
        sendNotification(request);
    }

    public void sendDeudaVencida(Map<String, String> vars, String toEmail, String toName, String tenantName, Long tenantId) {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .templateType("DEUDA_VENCIDA")
                .toEmail(toEmail)
                .toName(toName)
                .tenantName(tenantName)
                .tenantId(tenantId)
                .variables(vars)
                .build();
        sendNotification(request);
    }

    public void sendResumenDeudaVencida(Map<String, String> vars, String toEmail, String toName, String tenantName, Long tenantId) {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .templateType("RESUMEN_DEUDA_VENCIDA")
                .toEmail(toEmail)
                .toName(toName)
                .tenantName(tenantName)
                .tenantId(tenantId)
                .variables(vars)
                .build();
        sendNotification(request);
    }
}
