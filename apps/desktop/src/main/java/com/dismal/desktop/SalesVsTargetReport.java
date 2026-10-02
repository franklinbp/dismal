package com.dismal.desktop;

import java.time.LocalDate;
import java.util.List;

public record SalesVsTargetReport(
        LocalDate from,
        LocalDate to,
        long totalTargets,
        long achievedTargets,
        long totalActualUnits,
        List<SalesVsTargetItem> items
) {}
