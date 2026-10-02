package com.dismal.distribuciones.modules.expenses.dto;

import java.math.BigDecimal;

public record ExpenseRequest(
        String name,
        String category,
        String type,
        BigDecimal totalAmount,
        Integer months,
        Integer dueDay,
        String priority,
        Boolean active
) {}
