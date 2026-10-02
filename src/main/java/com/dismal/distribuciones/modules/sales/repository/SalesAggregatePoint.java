package com.dismal.distribuciones.modules.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SalesAggregatePoint {
    LocalDateTime getPeriod();
    BigDecimal getTotal();
    Long getCount();
}
