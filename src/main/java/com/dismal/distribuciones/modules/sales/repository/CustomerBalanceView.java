package com.dismal.distribuciones.modules.sales.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A projection interface for fetching customer pending balances efficiently.
 * Spring Data JPA will automatically implement this interface.
 */
public interface CustomerBalanceView {
    UUID getUserId();
    BigDecimal getTotalPending();
}
