package com.dismal.distribuciones.modules.reports.dto;

import java.time.LocalDate;
import java.util.List;

public record SalesVsTargetReportResponse(
        LocalDate from,
        LocalDate to,
        long totalTargets,
        long achievedTargets,
        long totalActualUnits,
        List<SalesVsTargetItemResponse> items
) {}
