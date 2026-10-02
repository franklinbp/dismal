package com.dismal.distribuciones.modules.planning.dto;

import java.math.BigDecimal;

public record SalesTargetSettingsResponse(
        BigDecimal fixedCostGlobal
) {}
