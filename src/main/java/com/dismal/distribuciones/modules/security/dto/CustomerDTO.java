package com.dismal.distribuciones.modules.security.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerDTO(
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
        Integer creditDays,
        boolean enabled
) {}
