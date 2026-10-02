package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountsReceivableSaleDetailResponse(
        UUID accountsReceivableId,
        UUID saleId,
        String products,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        LocalDate dueDate,
        AccountsReceivableStatus status,
        LocalDateTime saleCreatedAt
) {}
