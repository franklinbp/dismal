package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record StorefrontPaymentClaimRequest(
        @NotNull UUID paymentAccountId,
        @NotBlank @Size(max = 160) String payerName,
        @NotBlank @Size(max = 120) String sourceBank,
        @NotBlank @Size(max = 160) String reference,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}
