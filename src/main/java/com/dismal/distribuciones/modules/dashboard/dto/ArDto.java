package com.dismal.distribuciones.modules.dashboard.dto;

import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ArDto(
        UUID id,
        String clientName,
        String clientEmail,
        LocalDate dueDate,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        AccountsReceivableStatus status
) {}
