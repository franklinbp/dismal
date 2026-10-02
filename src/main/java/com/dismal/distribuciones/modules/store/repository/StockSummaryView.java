package com.dismal.distribuciones.modules.store.repository;

public interface StockSummaryView {
    java.util.UUID getSoftwareId();
    String getSoftwareName();
    Long getAvailableCount();
    Long getAssignedCount();
}
