package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentAccountType;

import java.util.UUID;

public record PaymentAccountResponse(
        UUID id,
        String name,
        PaymentAccountType type,
        String currency,
        boolean active,
        boolean defaultAccount,
        String countryCode,
        boolean publicForStorefront,
        String bankName,
        String accountHolder,
        String accountNumber,
        String accountType,
        String taxId
) {}
