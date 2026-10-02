package com.dismal.distribuciones.modules.admin.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.math.BigDecimal;

public record AdminUserRequest(
        String firstname,
        String lastname,
        String email,
        String phone,
        Role role,
        CustomerType customerType,
        StorefrontCountry country,
        Boolean enabled,
        String taxId,
        String billingEmail,
        Boolean hasCredit,
        BigDecimal creditLimit,
        Integer creditDays,
        String password
) {
    public AdminUserRequest(
            String firstname,
            String lastname,
            String email,
            String phone,
            Role role,
            CustomerType customerType,
            Boolean enabled,
            String taxId,
            String billingEmail,
            Boolean hasCredit,
            BigDecimal creditLimit,
            Integer creditDays,
            String password
    ) {
        this(firstname, lastname, email, phone, role, customerType, null, enabled,
                taxId, billingEmail, hasCredit, creditLimit, creditDays, password);
    }
}
