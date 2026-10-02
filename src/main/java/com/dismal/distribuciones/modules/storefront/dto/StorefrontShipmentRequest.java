package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorefrontShipmentRequest(
        @NotBlank @Size(max = 120) String carrier,
        @NotBlank @Size(max = 160) String trackingNumber
) {}
