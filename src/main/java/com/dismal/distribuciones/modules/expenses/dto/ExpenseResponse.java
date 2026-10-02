package com.dismal.distribuciones.modules.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        String name,
        String category,
        String type,
        BigDecimal totalAmount,
        Integer months,
        BigDecimal monthlyAmount,
        Integer dueDay,
        String priority,
        boolean active,
        LocalDateTime createdAt
) {}
