package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentAccountType;

public record PaymentAccountRequest(
        String name,
        PaymentAccountType type,
        String currency,
        Boolean active,
        Boolean defaultAccount,
        String countryCode,
        Boolean publicForStorefront,
        String bankName,
        String accountHolder,
        String accountNumber,
        String accountType,
        String taxId
) {}
