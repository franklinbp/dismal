package com.dismal.distribuciones.modules.planning.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RealActivationCostResponse(
        UUID softwareId,
        BigDecimal averageActivationCost
) {
}
