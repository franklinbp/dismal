package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorefrontShippingRequest(
        @NotBlank @Size(max = 180) String recipient,
        @NotBlank @Size(max = 220) String addressLine1,
        @Size(max = 220) String addressLine2,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String region,
        @Size(max = 32) String postalCode,
        @Size(max = 500) String notes
) {}
