package com.dismal.distribuciones.modules.security.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WholesaleApplicationRequest(
        @NotBlank String country,
        @NotBlank @Size(max = 180) String businessName,
        @NotBlank @Size(max = 40) String taxId,
        @NotBlank @Size(max = 32) String phone,
        @Size(max = 240) String website,
        @Size(max = 1000) String notes
) {}
