package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID saleId,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        UUID paymentAccountId,
        String paymentAccountName,
        LocalDateTime createdAt
) {}
