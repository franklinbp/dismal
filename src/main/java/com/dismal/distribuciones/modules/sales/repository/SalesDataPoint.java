package com.dismal.distribuciones.modules.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A projection interface for reporting sales data.
 * Spring Data JPA will automatically create proxy instances of this interface.
 */
public interface SalesDataPoint {
    LocalDateTime getDate();
    BigDecimal getTotalSales();
}
