package com.dismal.distribuciones.modules.expenses.dto;

import java.math.BigDecimal;

public record ExpenseSummaryResponse(
        BigDecimal monthPayments,
        BigDecimal monthExpenses,
        BigDecimal balance,
        String status,
        String recommendation
) {}
