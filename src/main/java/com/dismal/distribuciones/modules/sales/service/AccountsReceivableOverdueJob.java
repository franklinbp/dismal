package com.dismal.distribuciones.modules.sales.service;

import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountsReceivableOverdueJob {

    private final AccountsReceivableRepository accountsReceivableRepository;
    private final OutboxService outboxService;

    @Scheduled(cron = "${app.ar.overdue.cron:0 10 2 * * *}")
    @Transactional
    public void detectOverdue() {
        LocalDate today = LocalDate.now();
        List<AccountsReceivable> candidates = accountsReceivableRepository.findOverdueCandidates(
                today,
                List.of(AccountsReceivableStatus.OPEN, AccountsReceivableStatus.OVERDUE)
        );

        Map<UUID, ClientOverdue> overdueByClient = new HashMap<>();
        for (AccountsReceivable ar : candidates) {
            if (ar.getStatus() == AccountsReceivableStatus.OPEN) {
                ar.setStatus(AccountsReceivableStatus.OVERDUE);
            }
            overdueByClient.compute(ar.getClient().getId(), (key, existing) -> {
                if (existing == null) {
                    return new ClientOverdue(ar.getClient(), ar.getBalance(), ar.getDueDate());
                }
                existing.balance = existing.balance.add(ar.getBalance());
                if (ar.getDueDate().isBefore(existing.oldestDueDate)) {
                    existing.oldestDueDate = ar.getDueDate();
                }
                return existing;
            });
        }

        for (ClientOverdue overdue : overdueByClient.values()) {
            long daysOverdue = ChronoUnit.DAYS.between(overdue.oldestDueDate, today);
            if (daysOverdue < 0) {
                daysOverdue = 0;
            }
            UUID idempotencyKey = UUID.nameUUIDFromBytes(
                    ("AR_OVERDUE:" + overdue.client.getId() + ":" + today).getBytes(StandardCharsets.UTF_8));
            outboxService.enqueueArOverdueEvent(
                    overdue.client,
                    overdue.balance,
                    daysOverdue,
                    overdue.oldestDueDate,
                    idempotencyKey
            );
        }
    }

    private static class ClientOverdue {
        private final com.dismal.distribuciones.modules.security.domain.User client;
        private BigDecimal balance;
        private LocalDate oldestDueDate;

        private ClientOverdue(com.dismal.distribuciones.modules.security.domain.User client, BigDecimal balance, LocalDate oldestDueDate) {
            this.client = client;
            this.balance = balance != null ? balance : BigDecimal.ZERO;
            this.oldestDueDate = oldestDueDate;
        }
    }
}
