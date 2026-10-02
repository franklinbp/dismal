package com.dismal.distribuciones.modules.storefront.dto;

import java.util.List;

public record StorefrontAccountOrderResponse(
        StorefrontOrderResponse order,
        List<StorefrontLicenseResponse> licenses
) {}
