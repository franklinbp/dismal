package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a net profit report for a given date range.
 */
public record ProfitReportDTO(
        LocalDateTime startDate,
        LocalDateTime endDate,
        BigDecimal netProfit
) {}
