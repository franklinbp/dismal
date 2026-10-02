package com.dismal.desktop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesPeriodReport(
        LocalDate from,
        LocalDate to,
        String period,
        BigDecimal totalAmount,
        long totalCount,
        List<SalesPeriodPoint> points
) {}
