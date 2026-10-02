package com.dismal.distribuciones.modules.security.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

public record UpdateProfileRequest(
        StorefrontCountry country,
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 32) String phone,
        @Size(max = 40) String taxId,
        @Email @Size(max = 180) String billingEmail
) {}
