package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record QuoteSummaryResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientEmail,
        QuoteStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
