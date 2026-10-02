package com.dismal.distribuciones.modules.security.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.util.UUID;
import java.math.BigDecimal;

public record UserMeDTO(
        UUID id,
        String email,
        Role role,
        CustomerType customerType,
        StorefrontCountry country,
        String currency,
        boolean marketActive,
        String firstName,
        String lastName,
        String phone,
        String taxId,
        String billingEmail,
        boolean emailVerified,
        boolean hasCredit,
        BigDecimal creditLimit,
        BigDecimal creditUsed,
        BigDecimal creditAvailable,
        Integer creditDays
) {}
