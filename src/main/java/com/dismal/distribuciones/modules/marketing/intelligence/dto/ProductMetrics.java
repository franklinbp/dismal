package com.dismal.distribuciones.modules.marketing.intelligence.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductMetrics(
    Long sales7d,
    Long sales14d,
    Long sales30d,
    Long interactions, // Mensajes/Clics/Vistas
    Double conversionRate, // 0.0 to 1.0
    BigDecimal margin,
    LocalDate lastSaleDate
) {}
