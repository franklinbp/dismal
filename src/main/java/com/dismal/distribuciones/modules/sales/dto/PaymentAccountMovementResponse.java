package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentAccountMovementResponse(
        UUID id,
        UUID paymentAccountId,
        String paymentAccountName,
        UUID saleId,
        String clientName,
        String clientEmail,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        LocalDateTime createdAt
) {}
