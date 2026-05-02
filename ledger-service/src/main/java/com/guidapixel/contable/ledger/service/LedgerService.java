package com.guidapixel.contable.ledger.service;

import com.guidapixel.contable.ledger.client.AuthClient;
import com.guidapixel.contable.ledger.client.MpClient;
import com.guidapixel.contable.ledger.domain.model.*;
import com.guidapixel.contable.ledger.domain.repository.AccountMovementRepository;
import com.guidapixel.contable.ledger.domain.repository.ClientBalanceRepository;
import com.guidapixel.contable.ledger.domain.repository.OverdueReminderLogRepository;
import com.guidapixel.contable.ledger.web.dto.*;
import com.guidapixel.contable.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountMovementRepository movementRepository;
    private final ClientBalanceRepository balanceRepository;
    private final OverdueReminderLogRepository overdueReminderLogRepository;
    private final AuthClient authClient;
    private final MpClient mpClient;

    @Transactional
    public MovementResponse createMovementFromInvoice(InvoiceMovementRequest request) {
        AccountMovement movement = AccountMovement.builder()
                .clientId(request.getClientId())
                .type(MovementType.CARGO_FACTURA)
                .amount(request.getTotalAmount())
                .direction(MovementDirection.DEBIT)
                .description(request.getDescription())
                .invoiceId(request.getInvoiceId())
                .createdByUserId(0L)
                .build();

        movement.setTenantId(request.getTenantId());
        movementRepository.save(movement);
        recalculateBalance(request.getTenantId(), request.getClientId());

        return toResponse(movement);
    }

    @Transactional
    public MovementResponse createMovement(Long clientId, CreateMovementRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }

        AccountMovement movement = AccountMovement.builder()
                .clientId(clientId)
                .type(request.getType())
                .amount(request.getAmount())
                .direction(request.getDirection())
                .description(request.getDescription())
                .invoiceId(request.getInvoiceId())
                .dueDate(request.getDueDate())
                .createdByUserId(0L)
                .build();

        movement.setTenantId(tenantId);
        movementRepository.save(movement);
        recalculateBalance(tenantId, clientId);

        return toResponse(movement);
    }

    @Transactional
    public MovementResponse markAsPaid(Long movementId, MarkPaidRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }

        AccountMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado"));

        if (!movement.getTenantId().equals(tenantId)) {
            throw new RuntimeException("No tienes acceso a este movimiento");
        }

        if (movement.getPaidAt() != null) {
            throw new RuntimeException("El movimiento ya esta marcado como pagado");
        }

        movement.setPaidAt(request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now());
        movementRepository.save(movement);
        recalculateBalance(tenantId, movement.getClientId());

        return toResponse(movement);
    }

    @Transactional
    public MovementResponse createPaymentLinkForMovement(Long movementId) {
        AccountMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado"));

        if (movement.getPaidAt() != null) {
            throw new RuntimeException("El movimiento ya esta marcado como pagado");
        }

        if (movement.getMpPreferenceId() != null) {
            throw new RuntimeException("El movimiento ya tiene un payment link de MP generado");
        }

        Long tenantId = movement.getTenantId();
        if (!authClient.isMpEnabled(tenantId)) {
            throw new RuntimeException("MercadoPago no esta habilitado para este tenant");
        }

        Map<String, Object> mpResponse = mpClient.createPaymentLink(
                tenantId,
                movement.getClientId(),
                movementId,
                movement.getAmount(),
                movement.getDescription()
        );

        if (mpResponse == null) {
            throw new RuntimeException("Error creando payment link de MercadoPago");
        }

        String preferenceId = (String) mpResponse.get("preferenceId");
        String paymentLinkUrl = (String) mpResponse.get("paymentLinkUrl");

        movement.setMpPreferenceId(preferenceId);
        movement.setMpPaymentLinkUrl(paymentLinkUrl);
        movement.setMpStatus("pending");
        movementRepository.save(movement);

        return toResponse(movement);
    }

    @Transactional
    public MovementResponse markAsPaidByMp(String preferenceId) {
        AccountMovement movement = movementRepository.findByMpPreferenceId(preferenceId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado para preferenceId: " + preferenceId));

        if (movement.getPaidAt() != null) {
            throw new RuntimeException("El movimiento ya esta marcado como pagado");
        }

        movement.setPaidAt(LocalDateTime.now());
        movement.setMpStatus("approved");
        movementRepository.save(movement);
        recalculateBalance(movement.getTenantId(), movement.getClientId());

        return toResponse(movement);
    }

    public Page<MovementResponse> getMovements(Long clientId, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }

        return movementRepository.findByTenantIdAndClientIdOrderByCreatedAtDesc(tenantId, clientId, pageable)
                .map(this::toResponse);
    }

    public Page<MovementResponse> getMyMovements(Long clientId, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }

        return movementRepository.findByTenantIdAndClientIdOrderByCreatedAtDesc(tenantId, clientId, pageable)
                .map(this::toResponse);
    }

    public BalanceResponse getBalance(Long clientId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No se pudo determinar el tenant");
        }

        return balanceRepository.findByTenantIdAndClientId(tenantId, clientId)
                .map(b -> BalanceResponse.builder()
                        .clientId(b.getClientId())
                        .totalDebt(b.getTotalDebt())
                        .lastMovementAt(b.getLastMovementAt())
                        .build())
                .orElse(BalanceResponse.builder()
                        .clientId(clientId)
                        .totalDebt(BigDecimal.ZERO)
                        .build());
    }

    public BalanceResponse getMyBalance(Long clientId) {
        return getBalance(clientId);
    }

    private void recalculateBalance(Long tenantId, Long clientId) {
        List<AccountMovement> movements = movementRepository.findByTenantIdAndClientIdAndPaidAtIsNull(tenantId, clientId);

        BigDecimal totalDebt = movements.stream()
                .map(m -> m.getDirection() == MovementDirection.DEBIT ? m.getAmount() : m.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime lastMovementAt = movements.stream()
                .map(AccountMovement::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        ClientBalance balance = balanceRepository.findByTenantIdAndClientId(tenantId, clientId)
                .orElse(ClientBalance.builder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .totalDebt(BigDecimal.ZERO)
                        .build());

        balance.setTotalDebt(totalDebt);
        balance.setLastMovementAt(lastMovementAt);
        balanceRepository.save(balance);
    }

    private MovementResponse toResponse(AccountMovement movement) {
        return MovementResponse.builder()
                .id(movement.getId())
                .clientId(movement.getClientId())
                .type(movement.getType())
                .amount(movement.getAmount())
                .direction(movement.getDirection())
                .description(movement.getDescription())
                .invoiceId(movement.getInvoiceId())
                .dueDate(movement.getDueDate())
                .paidAt(movement.getPaidAt())
                .createdAt(movement.getCreatedAt())
                .mpPreferenceId(movement.getMpPreferenceId())
                .mpPaymentLinkUrl(movement.getMpPaymentLinkUrl())
                .mpStatus(movement.getMpStatus())
                .build();
    }

    public Map<String, Object> getUpcomingDueMovements() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate in3Days = today.plusDays(3);

        List<AccountMovement> movements = movementRepository.findUpcomingDue(today, in3Days);

        List<Map<String, Object>> result = movements.stream()
                .filter(m -> m.getDirection() == MovementDirection.DEBIT)
                .map(m -> {
                    long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(today, m.getDueDate());
                    return Map.<String, Object>of(
                            "id", m.getId(),
                            "tenantId", m.getTenantId(),
                            "clientId", m.getClientId(),
                            "clientEmail", m.getClientEmail() != null ? m.getClientEmail() : "",
                            "clientName", m.getClientName() != null ? m.getClientName() : "",
                            "description", m.getDescription(),
                            "amount", m.getAmount(),
                            "dueDate", m.getDueDate().toString(),
                            "diasRestantes", diasRestantes,
                            "paymentLinkUrl", m.getMpPaymentLinkUrl() != null ? m.getMpPaymentLinkUrl() : ""
                    );
                })
                .collect(Collectors.toList());

        return Map.of("movements", result);
    }

    public Map<String, Object> getOverdueMovements() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);

        List<AccountMovement> overdueMovements = movementRepository.findOverdueNotRecentlyReminded(today, sevenDaysAgo);

        if (overdueMovements.isEmpty()) {
            return Map.of("movements", List.of());
        }

        Set<Long> tenantIds = overdueMovements.stream()
                .map(AccountMovement::getTenantId)
                .collect(Collectors.toSet());

        Set<Long> enabledTenantIds = authClient.getOverdueReminderEnabledTenantIds(new ArrayList<>(tenantIds));

        List<Map<String, Object>> result = overdueMovements.stream()
                .filter(m -> enabledTenantIds.contains(m.getTenantId()))
                .map(m -> {
                    long diasVencido = java.time.temporal.ChronoUnit.DAYS.between(m.getDueDate(), today);
                    return Map.<String, Object>of(
                            "id", m.getId(),
                            "tenantId", m.getTenantId(),
                            "clientId", m.getClientId(),
                            "clientEmail", m.getClientEmail() != null ? m.getClientEmail() : "",
                            "clientName", m.getClientName() != null ? m.getClientName() : "",
                            "description", m.getDescription(),
                            "amount", m.getAmount(),
                            "dueDate", m.getDueDate().toString(),
                            "diasVencido", diasVencido,
                            "paymentLinkUrl", m.getMpPaymentLinkUrl() != null ? m.getMpPaymentLinkUrl() : ""
                    );
                })
                .collect(Collectors.toList());

        return Map.of("movements", result);
    }

    @Transactional
    public void logOverdueReminder(Long tenantId, Long clientId, Long movementId) {
        OverdueReminderLog log = OverdueReminderLog.builder()
                .tenantId(tenantId)
                .clientId(clientId)
                .movementId(movementId)
                .sentAt(LocalDateTime.now())
                .build();
        overdueReminderLogRepository.save(log);
    }
}
