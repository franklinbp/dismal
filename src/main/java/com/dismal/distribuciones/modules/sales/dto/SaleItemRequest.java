package com.dismal.distribuciones.modules.sales.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleItemRequest(
        UUID softwareId,
        Integer quantity,
        BigDecimal unitPrice,
        List<UUID> selectedLicenseIds
) {}
