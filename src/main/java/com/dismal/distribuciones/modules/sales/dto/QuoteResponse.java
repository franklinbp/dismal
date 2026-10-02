package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientEmail,
        QuoteStatus status,
        BigDecimal total,
        String notes,
        UUID saleId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<QuoteItemResponse> items
) {}
