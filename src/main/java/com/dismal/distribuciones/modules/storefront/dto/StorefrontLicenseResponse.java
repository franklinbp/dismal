package com.dismal.distribuciones.modules.storefront.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StorefrontLicenseResponse(
        UUID licenseId,
        UUID softwareId,
        String softwareName,
        String licenseKey,
        LocalDateTime assignedAt
) {}
