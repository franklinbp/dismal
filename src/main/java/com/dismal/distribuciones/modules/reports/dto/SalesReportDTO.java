package com.dismal.distribuciones.modules.reports.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a comprehensive sales report for a given period.
 */
public record SalesReportDTO(
        LocalDateTime startDate,
        LocalDateTime endDate,
        String period, // e.g., "day", "week", "month"
        List<SalesDataPointDTO> dataPoints
) {}
