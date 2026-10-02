package com.dismal.distribuciones.modules.sales.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SaleLicenseResponse(
        UUID id,
        UUID licenseId,
        String licenseKey,
        UUID softwareId,
        String softwareName,
        LocalDateTime assignedAt
) {}
