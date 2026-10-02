package com.dismal.distribuciones.modules.admin.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstname,
        String lastname,
        String email,
        String phone,
        Role role,
        CustomerType customerType,
        StorefrontCountry country,
        String currency,
        boolean primaryMarket,
        boolean enabled,
        boolean emailVerified,
        String taxId,
        String billingEmail,
        boolean hasCredit,
        BigDecimal creditLimit,
        BigDecimal creditUsed,
        Integer creditDays,
        ArSummaryResponse arSummary
) {}
