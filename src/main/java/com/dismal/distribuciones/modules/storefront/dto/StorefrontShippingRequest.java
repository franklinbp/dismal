package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.Size;

public record StorefrontShippingRequest(
        @Size(max = 180) String recipient,
        @Size(max = 220) String addressLine1,
        @Size(max = 220) String addressLine2,
        @Size(max = 120) String city,
        @Size(max = 120) String region,
        @Size(max = 32) String postalCode,
        @Size(max = 500) String notes
) {}
