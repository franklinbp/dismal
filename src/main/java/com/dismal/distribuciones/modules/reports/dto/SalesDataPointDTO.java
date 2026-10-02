package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a single data point for sales reports,
 * showing total sales for a specific date/period.
 */
public record SalesDataPointDTO(
        LocalDateTime date,
        BigDecimal totalSales
) {}
