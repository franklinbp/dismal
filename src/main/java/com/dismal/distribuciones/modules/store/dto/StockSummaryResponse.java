package com.dismal.distribuciones.modules.store.dto;

import java.util.UUID;

public record StockSummaryResponse(
        UUID softwareId,
        String softwareName,
        long available,
        long assigned
) {}
