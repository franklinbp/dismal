package com.dismal.distribuciones.modules.security.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for providing a summary of a customer, including their pending balance.
 */
public record CustomerSummaryDTO(
        UUID id,
        String fullName,
        String taxId,
        String phone,
        BigDecimal balancePending
) {
}
