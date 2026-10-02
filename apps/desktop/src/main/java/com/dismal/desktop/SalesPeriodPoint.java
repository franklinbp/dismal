package com.dismal.desktop;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesPeriodPoint(
        LocalDate period,
        BigDecimal total,
        long count
) {
    public LocalDate getPeriod() {
        return period;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public long getCount() {
        return count;
    }
}
