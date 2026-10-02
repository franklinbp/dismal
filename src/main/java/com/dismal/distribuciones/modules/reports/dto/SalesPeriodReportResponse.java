package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesPeriodReportResponse(
        LocalDate from,
        LocalDate to,
        String period,
        BigDecimal totalAmount,
        long totalCount,
        List<SalesPeriodPointResponse> points
) {}
