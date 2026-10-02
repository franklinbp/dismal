package com.dismal.distribuciones.modules.admin.dto;

import java.math.BigDecimal;

public record ArSummaryResponse(
        BigDecimal saldoActual,
        long diasAtraso,
        ArSummaryStatus estado
) {}
