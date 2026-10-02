package com.dismal.distribuciones.modules.store.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LicenseAssignmentResponse(
        UUID saleId,
        UUID clientId,
        String clientName,
        String clientEmail,
        LocalDateTime assignedAt
) {}
