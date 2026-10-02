package com.dismal.distribuciones.modules.dashboard.dto;

import com.dismal.distribuciones.modules.sales.domain.SaleStatus;
import com.dismal.distribuciones.modules.sales.domain.SaleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecentSaleDto(
        UUID id,
        LocalDateTime date,
        String clientName,
        String clientEmail,
        SaleType saleType,
        SaleStatus status,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance
) {}
