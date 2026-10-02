package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesPeriodPointResponse(
        LocalDate period,
        BigDecimal total,
        long count
) {}
