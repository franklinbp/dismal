package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorefrontCustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        @NotBlank String phone,
        String taxId,
        @Size(min = 8, max = 128) String password
) {}
