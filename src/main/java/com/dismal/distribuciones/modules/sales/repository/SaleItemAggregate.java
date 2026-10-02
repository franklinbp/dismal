package com.dismal.distribuciones.modules.sales.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface SaleItemAggregate {
    UUID getSoftwareId();
    String getSoftwareName();
    Long getUnits();
    BigDecimal getTotal();
}
