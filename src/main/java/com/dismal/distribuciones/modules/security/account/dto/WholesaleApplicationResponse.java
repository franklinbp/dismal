package com.dismal.distribuciones.modules.security.account.dto;

import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplicationStatus;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.time.LocalDateTime;
import java.util.UUID;

public record WholesaleApplicationResponse(
        UUID id,
        UUID userId,
        String customerEmail,
        String customerName,
        StorefrontCountry country,
        String businessName,
        String taxId,
        String phone,
        String website,
        String notes,
        WholesaleApplicationStatus status,
        UUID reviewedBy,
        String reviewNotes,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {}
