package com.guidapixel.contable.ledger.service;

import com.guidapixel.contable.ledger.client.NotificationClient;
import com.guidapixel.contable.ledger.domain.model.AccountMovement;
import com.guidapixel.contable.ledger.domain.model.FeeGenerationLog;
import com.guidapixel.contable.ledger.domain.model.MovementDirection;
import com.guidapixel.contable.ledger.domain.model.MovementType;
import com.guidapixel.contable.ledger.domain.model.RecurringFee;
import com.guidapixel.contable.ledger.domain.model.RecurringFeeOverride;
import com.guidapixel.contable.ledger.domain.repository.AccountMovementRepository;
import com.guidapixel.contable.ledger.domain.repository.FeeGenerationLogRepository;
import com.guidapixel.contable.ledger.domain.repository.RecurringFeeOverrideRepository;
import com.guidapixel.contable.ledger.domain.repository.RecurringFeeRepository;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeOverrideRequest;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeOverrideResponse;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeRequest;
import com.guidapixel.contable.ledger.web.dto.RecurringFeeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringFeeService {

    private final RecurringFeeRepository recurringFeeRepository;
    private final RecurringFeeOverrideRepository overrideRepository;
    private final AccountMovementRepository movementRepository;
    private final FeeGenerationLogRepository feeGenerationLogRepository;
    private final NotificationClient notificationClient;
    private final LedgerService ledgerService;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public RecurringFeeResponse getRecurringFee(Long tenantId, Long clientId) {
        RecurringFee fee = recurringFeeRepository.findByTenantIdAndClientId(tenantId, clientId)
                .orElseThrow(() -> new RuntimeException("No se encontro configuracion de honorarios para este cliente"));
        return toResponse(fee);
    }

    @Transactional
    public RecurringFeeResponse createOrUpdateRecurringFee(Long tenantId, Long clientId, RecurringFeeRequest request) {
        RecurringFee fee = recurringFeeRepository.findByTenantIdAndClientId(tenantId, clientId)
                .orElse(RecurringFee.builder()
                        .clientId(clientId)
                        .baseAmount(BigDecimal.ZERO)
                        .active(true)
                        .dayOfMonth(1)
                        .build());

        if (request.getBaseAmount() != null) {
            fee.setBaseAmount(request.getBaseAmount());
        }
        if (request.getActive() != null) {
            fee.setActive(request.getActive());
        }

        fee.setTenantId(tenantId);

        RecurringFee saved = recurringFeeRepository.save(fee);
        return toResponse(saved);
    }

    public BigDecimal resolveAmountForMonth(Long tenantId, Long clientId, String yearMonth) {
        return overrideRepository.findByTenantIdAndClientIdAndYearMonth(tenantId, clientId, yearMonth)
                .map(RecurringFeeOverride::getOverrideAmount)
                .orElseGet(() -> recurringFeeRepository.findByTenantIdAndClientId(tenantId, clientId)
                        .map(RecurringFee::getBaseAmount)
                        .orElse(BigDecimal.ZERO));
    }

    @Transactional
    public RecurringFeeOverrideResponse createOverride(Long tenantId, Long clientId, RecurringFeeOverrideRequest request) {
        RecurringFee fee = recurringFeeRepository.findByTenantIdAndClientId(tenantId, clientId)
                .orElseThrow(() -> new RuntimeException("No se encontro configuracion de honorarios para este cliente"));

        RecurringFeeOverride override = overrideRepository.findByRecurringFeeIdAndYearMonth(fee.getId(), request.getYearMonth())
                .orElse(RecurringFeeOverride.builder()
                        .recurringFeeId(fee.getId())
                        .clientId(clientId)
                        .build());

        override.setTenantId(tenantId);

        override.setYearMonth(request.getYearMonth());
        override.setOverrideAmount(request.getOverrideAmount());
        override.setReason(request.getReason());

        RecurringFeeOverride saved = overrideRepository.save(override);
        return toOverrideResponse(saved);
    }

    public List<RecurringFeeOverrideResponse> getOverrides(Long tenantId, Long clientId) {
        return overrideRepository.findByTenantIdAndClientIdAndYearMonth(tenantId, clientId, null).stream()
                .map(this::toOverrideResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public int generateMonthlyFees(String yearMonth) {
        List<RecurringFee> activeFees = recurringFeeRepository.findAll().stream()
                .filter(RecurringFee::isActive)
                .toList();

        return generateFeesForList(activeFees, yearMonth);
    }

    @Transactional
    public int generateMonthlyFeesForTenant(Long tenantId, String yearMonth) {
        List<RecurringFee> activeFees = recurringFeeRepository.findByTenantIdAndActiveTrue(tenantId);

        return generateFeesForList(activeFees, yearMonth);
    }

    private int generateFeesForList(List<RecurringFee> activeFees, String yearMonth) {
        int generated = 0;
        for (RecurringFee fee : activeFees) {
            try {
                if (isAlreadyGenerated(fee.getTenantId(), yearMonth)) {
                    log.info("Honorario ya generado para cliente {} en {}", fee.getClientId(), yearMonth);
                    logFeeGeneration(fee.getTenantId(), fee.getClientId(), yearMonth, BigDecimal.ZERO, true, "Ya generado");
                    continue;
                }

                BigDecimal amount = resolveAmountForMonth(fee.getTenantId(), fee.getClientId(), yearMonth);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Monto cero para cliente {} en {}, saltando", fee.getClientId(), yearMonth);
                    logFeeGeneration(fee.getTenantId(), fee.getClientId(), yearMonth, amount, true, "Monto cero");
                    continue;
                }

                AccountMovement movement = AccountMovement.builder()
                        .tenantId(fee.getTenantId())
                        .clientId(fee.getClientId())
                        .type(MovementType.CARGO_MANUAL)
                        .amount(amount)
                        .direction(MovementDirection.DEBIT)
                        .description("Honorarios mensuales - " + yearMonth)
                        .dueDate(java.time.YearMonth.parse(yearMonth).atEndOfMonth())
                        .createdByUserId(0L)
                        .build();

                movementRepository.save(movement);
                logFeeGeneration(fee.getTenantId(), fee.getClientId(), yearMonth, amount, true, null);

                sendHonorarioGeneradoEmail(fee, amount, yearMonth);

                generated++;
                log.info("Honorario generado para cliente {}: ${} - {}", fee.getClientId(), amount, yearMonth);
            } catch (Exception e) {
                log.error("Error generando honorario para cliente {}: {}", fee.getClientId(), e.getMessage());
                logFeeGeneration(fee.getTenantId(), fee.getClientId(), yearMonth, BigDecimal.ZERO, false, e.getMessage());
            }
        }

        return generated;
    }

    private void logFeeGeneration(Long tenantId, Long clientId, String yearMonth, BigDecimal amount, boolean success, String errorMessage) {
        FeeGenerationLog logEntry = FeeGenerationLog.builder()
                .clientId(clientId)
                .yearMonth(yearMonth)
                .amount(amount)
                .generatedAt(LocalDateTime.now())
                .success(success)
                .errorMessage(errorMessage)
                .build();
        logEntry.setTenantId(tenantId);
        feeGenerationLogRepository.save(logEntry);
    }

    private void sendHonorarioGeneradoEmail(RecurringFee fee, BigDecimal amount, String yearMonth) {
        try {
            String tenantName = fee.getTenantId().toString();
            String clientEmail = "";
            String clientName = "";

            if (fee.getClientEmail() != null && !fee.getClientEmail().isBlank()) {
                clientEmail = fee.getClientEmail();
            }
            if (fee.getClientName() != null && !fee.getClientName().isBlank()) {
                clientName = fee.getClientName();
            }

            if (clientEmail.isBlank()) {
                return;
            }

            String mes = yearMonth;
            String fechaVencimiento = LocalDate.parse(yearMonth + "-01").plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String portalUrl = appBaseUrl + "/mi-cuenta";

            Map<String, String> vars = Map.of(
                    "nombreEstudio", tenantName,
                    "nombreCliente", clientName,
                    "monto", amount.toString(),
                    "mes", mes,
                    "fechaVencimiento", fechaVencimiento,
                    "portalUrl", portalUrl
            );

            notificationClient.sendHonorarioGenerado(clientEmail, clientName, tenantName, fee.getTenantId(), vars);
        } catch (Exception e) {
            log.warn("Error enviando email de honorario generado: {}", e.getMessage());
        }
    }

    public boolean isAlreadyGenerated(Long tenantId, String yearMonth) {
        return feeGenerationLogRepository.findByTenantIdAndYearMonthAndSuccessTrue(tenantId, yearMonth).isPresent();
    }

    private RecurringFeeResponse toResponse(RecurringFee fee) {
        return RecurringFeeResponse.builder()
                .id(fee.getId())
                .clientId(fee.getClientId())
                .baseAmount(fee.getBaseAmount())
                .active(fee.isActive())
                .dayOfMonth(fee.getDayOfMonth())
                .createdAt(fee.getCreatedAt())
                .build();
    }

    private RecurringFeeOverrideResponse toOverrideResponse(RecurringFeeOverride override) {
        return RecurringFeeOverrideResponse.builder()
                .id(override.getId())
                .recurringFeeId(override.getRecurringFeeId())
                .clientId(override.getClientId())
                .yearMonth(override.getYearMonth())
                .overrideAmount(override.getOverrideAmount())
                .reason(override.getReason())
                .createdAt(override.getCreatedAt())
                .build();
    }
}
