package com.dismal.distribuciones.modules.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentAccountUsageResponse(
        PaymentAccountResponse account,
        BigDecimal recordedIncome,
        long paymentCount,
        LocalDateTime lastMovementAt
) {}
