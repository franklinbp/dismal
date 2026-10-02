package com.dismal.distribuciones.modules.store.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLicenseRequest(
        UUID softwareId,
        String licenseKey,
        BigDecimal purchasePrice,
        Integer maxActivations
) {}
