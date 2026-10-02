package com.dismal.distribuciones.modules.storefront.dto;

import java.util.UUID;

public record StorefrontPaymentAccountResponse(
        UUID id,
        String name,
        String country,
        String currency,
        String bankName,
        String accountHolder,
        String accountNumber,
        String accountType,
        String taxId
) {}
