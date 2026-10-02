package com.dismal.desktop;

public record AdminUser(
        String id,
        String firstname,
        String lastname,
        String email,
        String phone,
        String role,
        boolean enabled,
        String taxId,
        String billingEmail,
        boolean hasCredit,
        double creditLimit,
        double creditUsed,
        int creditDays,
        ArSummary arSummary
) {}
