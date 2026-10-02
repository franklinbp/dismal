package com.dismal.distribuciones.modules.store.dto;

import com.dismal.distribuciones.modules.store.domain.LicenseStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record LicenseResponse(
        UUID id,
        String licenseKey,
        LicenseStatus status,
        boolean available,
        Integer usedActivations,
        Integer maxActivations,
        BigDecimal purchasePrice,
        UUID softwareId,
        String softwareName,
        UUID ownerId,
        String ownerEmail
) {}
