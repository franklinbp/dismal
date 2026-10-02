package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StorefrontOrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer quantity
) {}
