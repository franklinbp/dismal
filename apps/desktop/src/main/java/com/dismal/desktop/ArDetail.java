package com.dismal.desktop;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArDetail(
        String id,
        String saleId,
        String clientId,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        LocalDate dueDate,
        String status
) {}
