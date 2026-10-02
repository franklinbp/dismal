package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.crm.service.CrmWhatsappNotificationService;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationSendResult;
import com.dismal.distribuciones.modules.integrations.notifications.service.EmailNotificationService;
import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationDeliveryLogService;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableClientSummaryResponse;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivablePaymentRequest;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableResponse;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableReminderResponse;
import com.dismal.distribuciones.modules.sales.dto.AccountsReceivableSaleDetailResponse;
import com.dismal.distribuciones.modules.sales.dto.PaymentRequest;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.security.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountsReceivableService {

    private final AccountsReceivableRepository accountsReceivableRepository;
    private final OutboxService outboxService;
    private final EmailNotificationService emailNotificationService;
    private final CrmWhatsappNotificationService whatsappNotificationService;
    private final NotificationDeliveryLogService deliveryLogService;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public List<AccountsReceivableResponse> listAll() {
        return accountsReceivableRepository.findAll().stream()
                .map(ar -> new AccountsReceivableResponse(
                        ar.getId(),
                        ar.getSale().getId(),
                        ar.getClient().getId(),
                        ar.getTotal(),
                        ar.getPaid(),
                        ar.getBalance(),
                        ar.getDueDate(),
                        ar.getStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountsReceivableResponse> listByClient(UUID clientId) {
        return accountsReceivableRepository.findByClientId(clientId).stream()
                .map(ar -> new AccountsReceivableResponse(
                        ar.getId(),
                        ar.getSale().getId(),
                        ar.getClient().getId(),
                        ar.getTotal(),
                        ar.getPaid(),
                        ar.getBalance(),
                        ar.getDueDate(),
                        ar.getStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountsReceivableResponse getBySaleId(UUID saleId) {
        return accountsReceivableRepository.findBySaleId(saleId)
                .map(ar -> new AccountsReceivableResponse(
                        ar.getId(),
                        ar.getSale().getId(),
                        ar.getClient().getId(),
                        ar.getTotal(),
                        ar.getPaid(),
                        ar.getBalance(),
                        ar.getDueDate(),
                        ar.getStatus()
                ))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AccountsReceivableClientSummaryResponse> listGroupedByClient(AccountsReceivableStatus status) {
        List<AccountsReceivable> records = accountsReceivableRepository
                .findByStatusAndBalanceGreaterThanOrderByDueDateAsc(status, BigDecimal.ZERO);
        return groupByClient(records);
    }

    @Transactional(readOnly = true)
    public AccountsReceivableClientSummaryResponse getClientDetail(UUID clientId, AccountsReceivableStatus status) {
        List<AccountsReceivable> records = accountsReceivableRepository
                .findByClientIdAndStatusAndBalanceGreaterThanOrderByDueDateAsc(clientId, status, BigDecimal.ZERO);
        return groupByClient(records).stream().findFirst().orElse(null);
    }

    @Transactional
    public AccountsReceivableReminderResponse sendClientReminder(UUID clientId, AccountsReceivableStatus status) {
        List<AccountsReceivable> records = accountsReceivableRepository
                .findByClientIdAndStatusAndBalanceGreaterThanOrderByDueDateAsc(clientId, status, BigDecimal.ZERO);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("Accounts receivable not found for client ID: " + clientId);
        }

        AccountsReceivableClientSummaryResponse summary = toClientSummary(records);
        AccountsReceivable first = records.get(0);
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("AR_CLIENT_REMINDER:" + clientId + ":" + status + ":" + System.currentTimeMillis())
                        .getBytes(StandardCharsets.UTF_8)
        );
        UUID outboxEventId = outboxService.enqueueArOverdueEvent(
                first.getClient(),
                summary.balance(),
                summary.maxDaysOverdue(),
                summary.oldestDueDate(),
                idempotencyKey
        );

        NotificationSendResult emailResult = emailNotificationService.sendArReminder(
                first.getClient(),
                summary.balance(),
                summary.maxDaysOverdue(),
                summary.oldestDueDate()
        );
        recordDelivery(outboxEventId, NotificationChannel.EMAIL, emailResult);

        NotificationSendResult whatsappResult = whatsappNotificationService.sendMessage(
                first.getClient().getPhone(),
                buildClientReminderMessage(summary, status),
                outboxEventId
        );

        String message = buildReminderMessage(emailResult, whatsappResult);
        return new AccountsReceivableReminderResponse(
                outboxEventId,
                first.getId(),
                first.getClient().getId(),
                summary.oldestDueDate(),
                summary.balance(),
                summary.maxDaysOverdue(),
                whatsappResult != null && whatsappResult.sent() ? "SENT" : "PROCESSED",
                message,
                emailResult != null && emailResult.sent(),
                whatsappResult != null && whatsappResult.sent(),
                emailResult != null ? emailResult.message() : "",
                whatsappResult != null ? whatsappResult.message() : ""
        );
    }

    @Transactional
    public AccountsReceivableClientSummaryResponse registerClientPayment(UUID clientId, AccountsReceivablePaymentRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        AccountsReceivableStatus status = request.status() != null ? request.status() : AccountsReceivableStatus.OVERDUE;
        PaymentMethod method = request.method() != null ? request.method() : PaymentMethod.CASH;
        List<AccountsReceivable> records = accountsReceivableRepository
                .findByClientIdAndStatusAndBalanceGreaterThanOrderByDueDateAsc(clientId, status, BigDecimal.ZERO);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("Accounts receivable not found for client ID: " + clientId);
        }
        BigDecimal totalBalance = sum(records, AmountField.BALANCE);
        if (request.amount().compareTo(totalBalance) > 0) {
            throw new BadRequestException("Payment amount cannot be greater than pending balance");
        }

        BigDecimal remaining = request.amount();
        for (AccountsReceivable ar : records) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal amountForSale = remaining.min(ar.getBalance());
            paymentService.addPayment(new PaymentRequest(
                    ar.getSale().getId(),
                    amountForSale,
                    method,
                    request.reference(),
                    request.paymentAccountId(),
                    request.shouldNotifyClient()
            ));
            remaining = remaining.subtract(amountForSale);
        }

        return getClientDetail(clientId, status);
    }

    @Transactional
    public AccountsReceivableReminderResponse sendReminder(UUID id) {
        AccountsReceivable ar = accountsReceivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounts receivable not found with ID: " + id));

        LocalDate today = LocalDate.now();
        long daysOverdue = 0;
        if (ar.getDueDate() != null) {
            daysOverdue = ChronoUnit.DAYS.between(ar.getDueDate(), today);
            if (daysOverdue < 0) {
                daysOverdue = 0;
            }
        }

        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("AR_REMINDER:" + ar.getId() + ":" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)
        );
        UUID outboxEventId = outboxService.enqueueArOverdueEvent(
                ar.getClient(),
                ar.getBalance(),
                daysOverdue,
                ar.getDueDate(),
                idempotencyKey
        );
        NotificationSendResult emailResult = emailNotificationService.sendArReminder(
                ar.getClient(),
                ar.getBalance(),
                daysOverdue,
                ar.getDueDate()
        );
        recordDelivery(outboxEventId, NotificationChannel.EMAIL, emailResult);

        NotificationSendResult whatsappResult = whatsappNotificationService.sendArReminder(
                ar.getClient(),
                ar.getBalance(),
                daysOverdue,
                ar.getDueDate(),
                outboxEventId
        );

        log.info("AR reminder processed arId={} clientId={} emailSent={} whatsappSent={}",
                ar.getId(),
                ar.getClient().getId(),
                emailResult != null && emailResult.sent(),
                whatsappResult != null && whatsappResult.sent());

        String message = buildReminderMessage(emailResult, whatsappResult);

        return new AccountsReceivableReminderResponse(
                outboxEventId,
                ar.getId(),
                ar.getClient().getId(),
                ar.getDueDate(),
                ar.getBalance(),
                daysOverdue,
                whatsappResult != null && whatsappResult.sent() ? "SENT" : "PROCESSED",
                message,
                emailResult != null && emailResult.sent(),
                whatsappResult != null && whatsappResult.sent(),
                emailResult != null ? emailResult.message() : "",
                whatsappResult != null ? whatsappResult.message() : ""
        );
    }

    private String buildReminderMessage(NotificationSendResult emailResult, NotificationSendResult whatsappResult) {
        boolean emailSent = emailResult != null && emailResult.sent();
        boolean whatsappSent = whatsappResult != null && whatsappResult.sent();
        if (emailSent && whatsappSent) {
            return "Recordatorio enviado por email y WhatsApp.";
        }
        if (whatsappSent) {
            return "Recordatorio enviado por WhatsApp.";
        }
        if (emailSent) {
            return "Recordatorio enviado por email. WhatsApp no fue enviado.";
        }
        String whatsappMessage = whatsappResult != null && whatsappResult.message() != null
                ? whatsappResult.message()
                : "WhatsApp no fue enviado.";
        String emailMessage = emailResult != null && emailResult.message() != null
                ? emailResult.message()
                : "Email no fue enviado.";
        return "No se pudo enviar el recordatorio. Email: " + emailMessage + " WhatsApp: " + whatsappMessage;
    }

    private void recordDelivery(UUID outboxEventId, NotificationChannel channel, NotificationSendResult result) {
        if (outboxEventId == null || result == null) {
            return;
        }
        if (result.sent()) {
            deliveryLogService.markSent(outboxEventId, channel, channel.name());
        } else {
            deliveryLogService.markFailed(outboxEventId, channel, result.message());
        }
    }

    private List<AccountsReceivableClientSummaryResponse> groupByClient(List<AccountsReceivable> records) {
        Map<UUID, List<AccountsReceivable>> grouped = records.stream()
                .collect(Collectors.groupingBy(ar -> ar.getClient().getId(), LinkedHashMap::new, Collectors.toList()));

        return grouped.values().stream()
                .map(this::toClientSummary)
                .sorted(Comparator
                        .comparing(AccountsReceivableClientSummaryResponse::oldestDueDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AccountsReceivableClientSummaryResponse::clientName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private AccountsReceivableClientSummaryResponse toClientSummary(List<AccountsReceivable> records) {
        User client = records.get(0).getClient();
        BigDecimal total = sum(records, AmountField.TOTAL);
        BigDecimal paid = sum(records, AmountField.PAID);
        BigDecimal balance = sum(records, AmountField.BALANCE);
        BigDecimal overdueBalance = records.stream()
                .filter(ar -> ar.getStatus() == AccountsReceivableStatus.OVERDUE)
                .map(AccountsReceivable::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal upcomingBalance = records.stream()
                .filter(ar -> ar.getStatus() == AccountsReceivableStatus.OPEN)
                .map(AccountsReceivable::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate oldestDueDate = records.stream()
                .map(AccountsReceivable::getDueDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate nextDueDate = records.stream()
                .map(AccountsReceivable::getDueDate)
                .filter(date -> date != null)
                .filter(date -> !date.isBefore(LocalDate.now()))
                .min(LocalDate::compareTo)
                .orElse(oldestDueDate);
        long maxDaysOverdue = records.stream()
                .filter(ar -> ar.getDueDate() != null)
                .mapToLong(ar -> Math.max(ChronoUnit.DAYS.between(ar.getDueDate(), LocalDate.now()), 0))
                .max()
                .orElse(0);

        return new AccountsReceivableClientSummaryResponse(
                client.getId(),
                fullName(client),
                client.getEmail(),
                client.getPhone(),
                records.size(),
                total,
                paid,
                balance,
                overdueBalance,
                upcomingBalance,
                oldestDueDate,
                nextDueDate,
                maxDaysOverdue,
                records.stream().map(this::toSaleDetail).toList()
        );
    }

    private AccountsReceivableSaleDetailResponse toSaleDetail(AccountsReceivable ar) {
        String products = ar.getSale().getItems().stream()
                .map(item -> item.getSoftware().getName() + " x" + item.getQuantity())
                .collect(Collectors.joining(", "));
        return new AccountsReceivableSaleDetailResponse(
                ar.getId(),
                ar.getSale().getId(),
                products,
                ar.getTotal(),
                ar.getPaid(),
                ar.getBalance(),
                ar.getDueDate(),
                ar.getStatus(),
                ar.getSale().getCreatedAt()
        );
    }

    private BigDecimal sum(List<AccountsReceivable> records, AmountField field) {
        return records.stream()
                .map(ar -> switch (field) {
                    case TOTAL -> ar.getTotal();
                    case PAID -> ar.getPaid();
                    case BALANCE -> ar.getBalance();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String fullName(User user) {
        String name = ((user.getFirstname() != null ? user.getFirstname() : "") + " " +
                (user.getLastname() != null ? user.getLastname() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String buildClientReminderMessage(AccountsReceivableClientSummaryResponse summary, AccountsReceivableStatus status) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hola ").append(summary.clientName()).append(",\n");
        if (status == AccountsReceivableStatus.OVERDUE) {
            builder.append("te recordamos que tienes saldos vencidos pendientes por $")
                    .append(formatMoney(summary.balance())).append(" USD.");
            if (summary.maxDaysOverdue() > 0) {
                builder.append("\nDias vencidos: ").append(summary.maxDaysOverdue());
            }
        } else {
            builder.append("te recordamos que tienes saldos por vencer por $")
                    .append(formatMoney(summary.balance())).append(" USD.");
        }
        builder.append("\n\nDetalle:");
        for (AccountsReceivableSaleDetailResponse sale : summary.sales()) {
            builder.append("\n- ")
                    .append(sale.products() == null || sale.products().isBlank() ? "Venta " + sale.saleId() : sale.products())
                    .append(": $").append(formatMoney(sale.balance()))
                    .append(" - vence ").append(sale.dueDate() != null ? sale.dueDate() : "sin fecha");
        }
        builder.append("\n\nSi ya realizaste el pago, comparte el comprobante. Gracias.");
        return builder.toString();
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private enum AmountField {
        TOTAL,
        PAID,
        BALANCE
    }
}
