package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AccountsReceivableResponse(
        UUID id,
        UUID saleId,
        UUID clientId,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        LocalDate dueDate,
        AccountsReceivableStatus status
) {}
