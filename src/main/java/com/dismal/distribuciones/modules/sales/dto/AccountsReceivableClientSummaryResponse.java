package com.dismal.distribuciones.modules.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AccountsReceivableClientSummaryResponse(
        UUID clientId,
        String clientName,
        String clientEmail,
        String clientPhone,
        int salesCount,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        BigDecimal overdueBalance,
        BigDecimal upcomingBalance,
        LocalDate oldestDueDate,
        LocalDate nextDueDate,
        long maxDaysOverdue,
        List<AccountsReceivableSaleDetailResponse> sales
) {}
