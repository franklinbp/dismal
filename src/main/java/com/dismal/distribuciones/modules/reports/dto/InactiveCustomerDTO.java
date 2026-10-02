package com.dismal.distribuciones.modules.reports.dto;

import java.util.UUID;

/**
 * DTO representing an inactive customer (registered but never purchased).
 */
public record InactiveCustomerDTO(
        UUID id,
        String fullName,
        String email,
        String phone
) {}
