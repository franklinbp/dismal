package com.dismal.distribuciones.modules.integrations.woocommerce.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WooDeliveredLicenseResponse(
        UUID fulfillmentId,
        UUID licenseId,
        String licenseKey,
        UUID softwareId,
        String softwareName,
        LocalDateTime assignedAt
) {}
