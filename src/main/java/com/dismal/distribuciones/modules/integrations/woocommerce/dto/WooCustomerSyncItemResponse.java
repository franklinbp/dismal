package com.dismal.distribuciones.modules.integrations.woocommerce.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;

import java.math.BigDecimal;
import java.util.UUID;

public record WooCustomerSyncItemResponse(
        UUID id,
        String firstname,
        String lastname,
        String phone,
        String email,
        String taxId,
        String billingEmail,
        CustomerType customerType,
        boolean hasCredit,
        BigDecimal creditLimit,
        BigDecimal creditUsed,
        Integer creditDays,
        boolean enabled
) {}
